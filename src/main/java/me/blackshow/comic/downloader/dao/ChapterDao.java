package me.blackshow.comic.downloader.dao;

import java.util.List;
import me.blackshow.comic.downloader.entity.Chapter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Title : ChapterDao Author : blackshow Date : 2018-11-08
 */
@Repository
public interface ChapterDao extends JpaRepository<Chapter, Long> {

    List<Chapter> findAllByCatalogId(Long catalogId);

}
