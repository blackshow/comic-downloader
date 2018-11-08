package me.blackshow.comic.downloader.dao;

import me.blackshow.comic.downloader.entity.Catalog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Title : CatalogDao Author : blackshow Date : 2018-11-08
 */
@Repository
public interface CatalogDao extends JpaRepository<Catalog,Long> {
}
