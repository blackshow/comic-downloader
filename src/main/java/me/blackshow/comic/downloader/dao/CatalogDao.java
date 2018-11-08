package me.blackshow.comic.downloader.dao;

import java.util.List;
import java.util.Optional;
import javax.transaction.Transactional;
import me.blackshow.comic.downloader.entity.Catalog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Title : CatalogDao Author : blackshow Date : 2018-11-08
 */
@Repository
@Transactional
public interface CatalogDao extends JpaRepository<Catalog, Long> {

    List<Catalog> findAllByComicId(Long comicId);

    Optional<Catalog> findTopByIdBeforeAndComicIdOrderByIdDesc(Long id, Long comicId);

    Optional<Catalog> findTopByIdAfterAndComicIdOrderByIdAsc(Long id, Long comicId);

    @Modifying
    @Query(value = "update Catalog set lastRead = true where id = :id")
    int markLastRead(@Param("id") Long id);

    @Modifying
    @Query(value = "update Catalog set lastRead = false where comicId = ?2 and id <> ?1")
    int revertOtherLastRead(@Param("id") Long id, @Param("comicId") Long comicId);
}
