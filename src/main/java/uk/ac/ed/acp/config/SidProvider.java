package uk.ac.ed.acp.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SidProvider {

    private final String sid;

    public SidProvider(@Value("${spring.datasource.url}") String datasourceUrl) {
        String query = datasourceUrl.substring(datasourceUrl.indexOf('?') + 1);
        for (String param : query.split("&")) {
            if (param.startsWith("currentSchema=")) {
                this.sid = param.split("=", 2)[1];
                return;
            }
        }
        throw new IllegalStateException(
                "Could not extract SID from connection string: " + datasourceUrl
        );
    }

    public String getSid() {
        return sid;
    }
}
