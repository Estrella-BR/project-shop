package estrella.tienda.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.WaitUntilState;
import estrella.tienda.model.Product;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


//It does not work on
@Service
public class DiaScrapService {

    private static final String URL = "https://www.dia.es/";
    private static final Path SESSION_FILE = Paths.get("dia-session.json");

    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                    "(KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36";

    private static final Pattern QUANTITY_PATTERN = Pattern.compile(
            "\\b\\d+(?:[,.]\\d+)?\\s?(?:kg|g|mg|ml|cl|l|ud|uds|unidades)\\b",
            Pattern.CASE_INSENSITIVE
    );

    public List<Product> extractFromAllCategories() {

        List<Product> allProducts = new ArrayList<>();

        try (Playwright playwright = Playwright.create()) {

            Browser browser = playwright.chromium().launch(
                    new BrowserType.LaunchOptions()
                            .setHeadless(true)
                            .setChannel("chrome")
                            .setArgs(List.of("--disable-blink-features=AutomationControlled"))
            );

            try {
                BrowserContext context = createContext(browser);
                try {
                    Page page = context.newPage();
                    applyStealth(page);

                    page.navigate(URL, new Page.NavigateOptions()
                            .setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
                    page.waitForTimeout(2000);
                    acceptCookiesIfPresent(page);

                    List<String> categoryUrls = getCategoriesFromApi(page);
                    if (categoryUrls.isEmpty()) {
                        System.err.println("No se han encontrado categorías DIA.");
                        return allProducts;
                    }

                    Set<String> seen = new HashSet();

                    for (String categoryUrl : categoryUrls) {
                        try {
                            List<Product> products = obtenerProductosCategoria(page, categoryUrl);
                            for (Product p : products) {
                                String id = p.getName().trim().toLowerCase()
                                        + "_" + p.getPrice().trim().toLowerCase();
                                if (seen.add(id)) {
                                    allProducts.add(p);
                                }
                            }
                        } catch (Exception e) {
                            System.err.println("Error procesando categoría: " + categoryUrl);
                            e.printStackTrace();
                        }
                    }

                    persistSession(context);

                } finally {
                    context.close();
                }
            } finally {
                browser.close();
            }
        }

        return allProducts;
    }

    private BrowserContext createContext(Browser browser) {

        Browser.NewContextOptions options = new Browser.NewContextOptions()
                .setUserAgent(USER_AGENT)
                .setViewportSize(1440, 900)
                .setLocale("es-ES");

        if (Files.exists(SESSION_FILE)) {
            options.setStorageStatePath(SESSION_FILE);
        }

        return browser.newContext(options);
    }

    private void applyStealth(Page page) {
        page.addInitScript("""
                Object.defineProperty(navigator, 'webdriver', { get: () => undefined });
                delete window.__playwright__binding__;
                delete window.__pwInitScripts;
                """);
    }

    private void persistSession(BrowserContext context) {
        try {
            context.storageState(
                    new BrowserContext.StorageStateOptions().setPath(SESSION_FILE));
        } catch (Exception e) {
            System.err.println("No se pudo persistir la sesión: " + e.getMessage());
        }
    }

    private void acceptCookiesIfPresent(Page page) {
        try {
            page.waitForTimeout(1500);

            Object clicked = page.evaluate("""
                () => {
                    // Selectores que usa OneTrust para el botón "Allow All"
                    const selectors = [
                        '#onetrust-accept-btn-handler',
                        'button#onetrust-accept-btn-handler',
                        '.onetrust-close-btn-handler',
                        'button[aria-label="Aceptar todas las cookies"]',
                        'button[aria-label="Accept All Cookies"]'
                    ];
                    for (const sel of selectors) {
                        const btn = document.querySelector(sel);
                        if (btn && btn.offsetParent !== null) {
                            btn.click();
                            return sel;
                        }
                    }
                    return null;
                }
                """);

            if (clicked != null) {
                try {
                    page.waitForSelector("#onetrust-banner-sdk",
                            new Page.WaitForSelectorOptions()
                                    .setState(com.microsoft.playwright.options.WaitForSelectorState.HIDDEN)
                                    .setTimeout(5000));
                } catch (TimeoutError ignored) { }
                page.waitForTimeout(1000);
            }

        } catch (Exception e) {
            System.err.println("Error gestionando cookies: " + e.getMessage());
        }
    }

    private void scrollUntilAllProductsLoaded(Page page) {

        final String productSelector = "li[data-test-id='product-card-list-item']";

        long previousCount = -1;
        int stableRounds = 0;

        for (int i = 0; i < 50; i++) {

            page.evaluate("window.scrollTo(0, document.body.scrollHeight)");
            page.waitForTimeout(1200);

            long currentCount = ((Number) page.evaluate("""
                () => document.querySelectorAll('li[data-test-id="product-card-list-item"]').length
                """)).longValue();

            if (currentCount == previousCount) {
                stableRounds++;
                if (stableRounds >= 2) {
                    break;
                }
            } else {
                stableRounds = 0;
            }

            previousCount = currentCount;

            try {
                Object moreClicked = page.evaluate("""
                    () => {
                        const btn = document.querySelector(
                            'button[data-test-id="load-more"], button.show-more, button[aria-label*="más"]'
                        );
                        if (btn) { btn.click(); return true; }
                        return false;
                    }
                    """);
                if (Boolean.TRUE.equals(moreClicked)) {
                    page.waitForTimeout(1500);
                }
            } catch (Exception ignored) { }
        }

        page.evaluate("window.scrollTo(0, 0)");
        page.waitForTimeout(500);
    }


    private void navigateAndWait(Page page, String url, String selectorToWaitFor) {

        page.navigate(url, new Page.NavigateOptions()
                .setWaitUntil(WaitUntilState.DOMCONTENTLOADED));

        try {
            if (selectorToWaitFor != null && !selectorToWaitFor.isBlank()) {
                page.waitForSelector(selectorToWaitFor,
                        new Page.WaitForSelectorOptions().setTimeout(20_000));
            } else {
                page.waitForLoadState(LoadState.LOAD);
            }
        } catch (TimeoutError e) {
            System.out.println("Timeout esperando '" + selectorToWaitFor + "' en " + url
                    + " — continuamos con el HTML disponible.");
        }

        page.waitForTimeout(1500);
    }


    private List<String> getCategoriesFromApi(Page page) {

        if (!page.url().startsWith(URL)) {
            page.navigate(URL, new Page.NavigateOptions()
                    .setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
            page.waitForTimeout(2000);
        }

        Object result = page.evaluate("""
            async () => {
                const response = await fetch('/api/v1/common-aggregator/menu-data', {
                    method: 'GET',
                    headers: { 'Accept': 'application/json, text/plain, */*' }
                });
                if (!response.ok) {
                    throw new Error('HTTP ' + response.status);
                }
                return await response.json();
            }
            """);

        Gson gson = new Gson();
        JsonObject root = gson.toJsonTree(result).getAsJsonObject();

        if (!root.has("categories") || !root.get("categories").isJsonArray()) {
            System.err.println("Respuesta inesperada del menú DIA: no hay 'categories'.");
            return List.of();
        }

        Set<String> categoryUrls = new LinkedHashSet<>();
        JsonArray categories = root.getAsJsonArray("categories");

        for (JsonElement el : categories) {
            if (!el.isJsonObject()) continue;
            JsonObject cat = el.getAsJsonObject();

            if (!cat.has("link") || !cat.get("link").isJsonPrimitive()) continue;

            String link = cat.get("link").getAsString().trim();
            if (link.isEmpty()) continue;

            if (link.startsWith("/")) {
                link = URL.replaceAll("/$", "") + link;
            }

            categoryUrls.add(link);
        }


        return new ArrayList<>(categoryUrls);
    }

    @SuppressWarnings("unchecked")
    public List<Product> obtenerProductosCategoria(Page page, String categoryUrl) {

        List<Product> products = new ArrayList<>();

        navigateAndWait(page, categoryUrl,
                "li[data-test-id=product-card-list-item]");

        scrollUntilAllProductsLoaded(page);

        List<Map<String, Object>> raw = (List<Map<String, Object>>) page.evaluate("""
                () => {
                    const cards = document.querySelectorAll(
                        'li[data-test-id="product-card-list-item"]'
                    );
                    return Array.from(cards).map(card => {
                        const nameEl  = card.querySelector('[data-test-id="search-product-card-name"]');
                        const priceEl = card.querySelector('[data-test-id="search-product-card-unit-price"]');
                        const imgEl   = card.querySelector('[data-test-id="search-product-card-image"]');
                        const linkEl  = card.querySelector('a[data-test-id="search-product-card-image-url"]');

                        let imgUrl = '';
                        if (imgEl) {
                            imgUrl = imgEl.src
                                  || imgEl.getAttribute('data-src')
                                  || imgEl.getAttribute('data-lazy-src')
                                  || '';
                        }

                        return {
                            name:  nameEl  ? nameEl.innerText.trim()  : '',
                            price: priceEl ? priceEl.innerText.replace(/\\u00a0/g, ' ').trim() : '',
                            image: imgUrl,
                            url:   linkEl  ? linkEl.href : ''
                        };
                    });
                }
                """);

        for (Map<String, Object> row : raw) {

            String name  = str(row.get("name"));
            String price = str(row.get("price"));
            String image = str(row.get("image"));
            String url   = str(row.get("url"));

            if (name.isBlank() || price.isBlank()) {
                continue;
            }

            String quantity = extraerCantidad(name);

            Product product = new Product(name, price, quantity, image, url, "Dia");

            product.generateId();
            products.add(product);
        }

        return products;
    }

    private String str(Object o) {
        return o == null ? "" : o.toString();
    }

    private String extraerCantidad(String name) {
        if (name == null || name.isBlank()) return "";
        Matcher m = QUANTITY_PATTERN.matcher(name);
        return m.find() ? m.group().trim() : "";
    }
}