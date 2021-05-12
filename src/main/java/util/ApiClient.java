package util;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.util.Arrays;

public class ApiClient {
    private final static Logger LOGGER = Logger.getLogger(ApiClient.class);
    public static String makeAPICall(String url) throws IOException {
        String response_content;

        CloseableHttpClient client = HttpClients.createDefault();
        HttpGet request = new HttpGet(url);

        try {
            CloseableHttpResponse response = client.execute(request);
            Header[] headers = response.getAllHeaders();
            String rateLimitUsed = Arrays.stream(headers)
                    .filter(header -> header.getName().equalsIgnoreCase("x-mbx-used-weight-1m"))
                    .findFirst()
                    .get().getValue();
            LOGGER.info("1 minute request uses: " + rateLimitUsed);
            if (Integer.parseInt(rateLimitUsed) > 1100 || response.getStatusLine().getStatusCode() == 429) {
                LOGGER.error("BREAKING API LIMIT - PAUSING FOR 2 MINUTES");
                try {
                    Thread.sleep(120000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            HttpEntity entity = response.getEntity();
            response_content = EntityUtils.toString(entity);
            EntityUtils.consume(entity);
        } catch (SSLException e) {
            response_content = "";
        }
        return response_content;
    }
}
