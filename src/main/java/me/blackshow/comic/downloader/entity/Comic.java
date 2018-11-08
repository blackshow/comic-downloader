package me.blackshow.comic.downloader.entity;

import lombok.Data;
import org.springframework.stereotype.Repository;

/**
 * Title : Comic Author : blackshow Date : 2018-11-08
 */
@Data
@Entity
public class Comic {

    private String name;

    private String url;

    private String thumbnail;

    private int thumbHeight = 280;

    private int thumbWidth = 210;

}
