package me.blackshow.comic.downloader;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ComicDownloaderApplication {

    public static void main(String[] args) {
        SpringApplication.run(ComicDownloaderApplication.class, args);
    }
}
