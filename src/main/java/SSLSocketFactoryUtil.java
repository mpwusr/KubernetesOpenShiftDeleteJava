import javax.net.ssl.*;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.cert.Certificate;

public class SSLSocketFactoryUtil {

    public static SSLSocketFactory fromCAFile(String caCertPath) throws Exception {
        TrustManagerFactory tmf = trustManagerFromCA(caCertPath);
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, tmf.getTrustManagers(), new SecureRandom());
        return sslContext.getSocketFactory();
    }

    public static TrustManagerFactory trustManagerFromCA(String caCertPath) throws Exception {
        // Load the CA certificate
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        InputStream caInput = new FileInputStream(caCertPath);
        Certificate ca;
        try {
            ca = cf.generateCertificate(caInput);
            System.out.println("✅ Loaded CA cert from " + caCertPath);
        } finally {
            caInput.close();
        }

        // Create a keystore and load the CA certificate into it
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, null);
        keyStore.setCertificateEntry("ca-cert", ca);

        // Initialize a TrustManager with the CA cert keystore
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(keyStore);

        return tmf;
    }
}
