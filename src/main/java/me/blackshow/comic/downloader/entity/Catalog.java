package me.blackshow.comic.downloader.entity;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Data;

/**
 * Title : Catalog Author : blackshow Date : 2018-11-08
 */
@Data
@Entity
@Table(name = "catalog")
public class Catalog implements Serializable {
    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, columnDefinition = "text")
    private String url;

    @Column(nullable = false, length = 20)
    private Long comicId;

}
