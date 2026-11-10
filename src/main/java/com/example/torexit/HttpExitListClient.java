package com.example.torexit;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Downloads the list from the Tor Project over HTTP. */
@Component
class HttpExitListClient implements ExitListClient {

    private final RestClient restClient;
    private final String url;

    HttpExitListClient(@Value("${tor.exit-list.url}") String url,
                       @Value("${tor.exit-list.connect-timeout}") Duration connectTimeout,
                       @Value("${tor.exit-list.read-timeout}") Duration readTimeout) {
        // Without timeouts a hanging server would block the refresh forever.
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeout);
        requestFactory.setReadTimeout(readTimeout);

        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
        this.url = url;
    }

    @Override
    public String fetch() {
        // A non-2xx answer makes RestClient throw an exception, so the caller never sees an error page as data.
        return restClient.get().uri(url).retrieve().body(String.class);
    }
}
