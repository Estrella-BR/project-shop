package estrella.tienda.component;

import estrella.tienda.service.ScrapService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class ScrapingStartupRunner implements ApplicationRunner {

    private final ScrapService scrapService;

    public ScrapingStartupRunner(ScrapService scrapService) {
        this.scrapService = scrapService;
    }

    @Override
    public void run(ApplicationArguments args) {
        // Obtener libros
        /*
        try {
            System.out.println("Iniciando scraping de libros...");

            scrapService.getAllBooks();

            System.out.println("Scraping de libros finalizado.");

        } catch (Exception e) {
            System.err.println("Error obteniendo los libros:");
            e.printStackTrace();
        }
*/
        // Obtener productos de Mercadona
      /*  try {
            System.out.println("Iniciando scraping de productos...");

            scrapService.getAllProducts();

            System.out.println("Scraping de productos finalizado.");

        } catch (Exception e) {
            System.err.println("Error obteniendo los productos:");
            e.printStackTrace();
        }

       */

    }
}
