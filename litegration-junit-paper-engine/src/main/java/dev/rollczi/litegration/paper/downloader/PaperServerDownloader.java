package dev.rollczi.litegration.paper.downloader;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;

public class PaperServerDownloader {

    private static final String PAPER_API_URL = "https://api.papermc.io/v2/projects/paper";

    public static Path download(String minecraftVersion) {
        try {
            HttpClient httpClient = HttpClient.newHttpClient();
            return tryDownload(minecraftVersion, httpClient);
        } catch (IOException | InterruptedException exception) {
            throw new RuntimeException(exception);
        }
    }

    private static Path tryDownload(String mcVersion, HttpClient client) throws IOException, InterruptedException {
        String latestBuild = fetchLatestBuild(mcVersion, client);
        String fileName = String.format("paper-%s-%s.jar", mcVersion, latestBuild);
        Path filePath = Path.of(fileName);
        if (!Files.exists(filePath)) {
            fetchJarFile(mcVersion, client, latestBuild, fileName, filePath);
        }
        return filePath;
    }

    private static void fetchJarFile(String mcVersion, HttpClient client, String latestBuild, String fileName, Path filePath) throws IOException, InterruptedException {
        String downloadUrl = String.format("%s/versions/%s/builds/%s/downloads/%s", PAPER_API_URL, mcVersion, latestBuild, fileName);
        HttpRequest downloadRequest = HttpRequest.newBuilder()
            .uri(URI.create(downloadUrl))
            .build();
        HttpResponse<Path> downloadResponse = client.send(downloadRequest, HttpResponse.BodyHandlers.ofFile(filePath));

        if (downloadResponse.statusCode() != 200) {
            throw new IOException("Failed to download Paper server. Status: " + downloadResponse.statusCode());
        }
    }

    private static String fetchLatestBuild(String mcVersion, HttpClient client) throws IOException, InterruptedException {
        HttpRequest versionRequest = HttpRequest.newBuilder()
            .uri(URI.create(String.format("%s/versions/%s", PAPER_API_URL, mcVersion)))
            .build();

        HttpResponse<String> versionResponse = client.send(versionRequest, HttpResponse.BodyHandlers.ofString());
        if (versionResponse.statusCode() != 200) {
            throw new IOException("Can not fetch latest build for version " + mcVersion + "! Status: " + versionResponse.statusCode());
        }

        String responseBody = versionResponse.body();
        String buildsArray = responseBody.substring(responseBody.indexOf("\"builds\":[") + 9);
        buildsArray = buildsArray.substring(0, buildsArray.indexOf("]"));
        String[] builds = buildsArray.split(",");
        return builds[builds.length - 1];
    }

}
