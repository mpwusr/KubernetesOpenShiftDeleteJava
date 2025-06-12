import io.github.cdimascio.dotenv.Dotenv;
import okhttp3.*;

import javax.net.ssl.X509TrustManager;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.util.Map;

public class OpenShiftApiDelete {
    public static void main(String[] args) throws Exception {
        Dotenv dotenv = Dotenv.configure().filename(".env").load();

        String token = dotenv.get("BEARER_TOKEN");
        String caCertPath = dotenv.get("CA_CERT_PATH", "./openshift-ca.crt");
        String apiServer = dotenv.get("API_SERVER", "https://127.0.0.1:6443");
        String namespace = dotenv.get("NAMESPACE", "test");
        String deploymentUriStr = dotenv.get("DEPLOYMENT_URI",
                "https://raw.githubusercontent.com/kubernetes/website/main/content/en/examples/controllers/nginx-deployment.yaml");

        URI deploymentUri = URI.create(deploymentUriStr);
        Map<String, Object> obj;

        if (deploymentUri.getScheme().startsWith("http")) {
            OkHttpClient fetchClient = new OkHttpClient();
            Request fetchRequest = new Request.Builder().url(deploymentUriStr).build();
            try (Response response = fetchClient.newCall(fetchRequest).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    throw new RuntimeException("Failed to fetch deployment file");
                }
                try (InputStream inputStream = response.body().byteStream()) {
                    ObjectMapper yamlReader = new ObjectMapper(new YAMLFactory());
                    obj = yamlReader.readValue(inputStream, Map.class);
                }
            }
        } else if (deploymentUri.getScheme().equals("file")) {
            try (InputStream inputStream = Files.newInputStream(Paths.get(deploymentUri))) {
                ObjectMapper yamlReader = new ObjectMapper(new YAMLFactory());
                obj = yamlReader.readValue(inputStream, Map.class);
            }
        } else {
            throw new IllegalArgumentException("Unsupported URI scheme for DEPLOYMENT_URI: " + deploymentUriStr);
        }

        // Convert parsed YAML to JSON string
        String jsonBody = new ObjectMapper().writeValueAsString(obj);

        OkHttpClient client = new OkHttpClient.Builder()
                .sslSocketFactory(
                        SSLSocketFactoryUtil.fromCAFile(caCertPath),
                        (X509TrustManager) SSLSocketFactoryUtil.trustManagerFromCA(caCertPath).getTrustManagers()[0]
                )
                .build();

        RequestBody body = RequestBody.create(jsonBody, MediaType.parse("application/json"));

        Request request = new Request.Builder()
                .url(apiServer + "/apis/apps/v1/namespaces/" + namespace + "/deployments")
                .addHeader("Authorization", "Bearer " + token)
                .addHeader("Content-Type", "application/json")
                .delete(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            System.out.println("Status: " + response.code());
            System.out.println("Body: " + (response.body() != null ? response.body().string() : "No response body"));
        }
    }
}
