package me.blackshow.comic.downloader.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@ConfigurationProperties(prefix = "data")
@Component
public class ComicsConfig {

    private String comics;
}
