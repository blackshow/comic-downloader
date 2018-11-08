package me.blackshow.comic.downloader.dao;

import java.util.Optional;
import me.blackshow.comic.downloader.entity.Comic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Title : ComicDao Author : blackshow Date : 2018-11-08
 */
@Repository
public interface ComicDao extends JpaRepository<Comic,Long> {

    Optional<Comic> findByName(String name);
}
