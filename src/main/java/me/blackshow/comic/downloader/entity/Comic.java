package me.blackshow.comic.downloader.entity;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Data;

/**
 * Title : Comic Author : blackshow Date : 2018-11-08
 */
@Data
@Entity
@Table(name = "comic")
public class Comic implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50,unique = true)
    private String name;

    @Column(nullable = false, columnDefinition = "text")
    private String url;

    @Column(columnDefinition = "text")
    private String thumbnail;

    private int thumbHeight = 280;

    private int thumbWidth = 210;

}
