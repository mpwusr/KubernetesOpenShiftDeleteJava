import okhttp3.*;

public class OpenshiftDeleteClass {
    public static void main(String[] args) throws Exception {
        String apiServer = "https://api.openshift.example.com:6443";
        String namespace = "default";
        String resource = "deployments";
        String name = "example-deployment";
        String token = System.getenv("BEARER_TOKEN");

        OkHttpClient client = new OkHttpClient.Builder()
            .sslSocketFactory(SSLSocketFactoryUtil.fromCAFile("/path/to/ca.crt"),
                              SSLSocketFactoryUtil.trustManagerFromCA("/path/to/ca.crt"))
            .build();

        Request request = new Request.Builder()
            .url(apiServer + "/apis/apps/v1/namespaces/" + namespace + "/" + resource + "/" + name)
            .delete()
            .addHeader("Authorization", "Bearer " + token)
            .addHeader("Accept", "application/json")
            .build();

        try (Response response = client.newCall(request).execute()) {
            System.out.println("Status: " + response.code());
            System.out.println("Body: " + response.body().string());
        }
    }
}
