package estrella.tienda.service;
import org.springframework.stereotype.Service;
import java.io.*;

@Service
public class ImageProcessingService {

    public void removeBackground(String inputPath, String outputPath)
            throws IOException, InterruptedException {

        ProcessBuilder processBuilder = new ProcessBuilder(
                "python",
                "remove_background.py",
                inputPath,
                outputPath
        );

        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {

            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
        }

        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new RuntimeException(
                    "Error al eliminar el fondo de la imagen"
            );
        }
    }
}
