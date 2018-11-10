package me.blackshow.comic.downloader.controller;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import me.blackshow.comic.downloader.config.ComicsConfig;
import me.blackshow.comic.downloader.dao.CatalogDao;
import me.blackshow.comic.downloader.dao.ChapterDao;
import me.blackshow.comic.downloader.dao.ComicDao;
import me.blackshow.comic.downloader.entity.Catalog;
import me.blackshow.comic.downloader.entity.Chapter;
import me.blackshow.comic.downloader.entity.Comic;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class IndexController {

    private static final Logger logger = LoggerFactory.getLogger(IndexController.class);
    private static final Map<String, String> COMICS = new HashMap<>();
    private static final String BASE_URL = "http://www.gufengmh.com";
    private static final String IMAGE_URL = "http://res.gufengmh.com/";
    private static final int TIMEOUT = 1000;

    private final ComicDao comicDao;

    private final CatalogDao catalogDao;

    private final ChapterDao chapterDao;

    private final ComicsConfig comicsConfig;

    @Autowired
    public IndexController(ComicDao comicDao, CatalogDao catalogDao, ChapterDao chapterDao,
        ComicsConfig comicsConfig) {
        this.comicDao = comicDao;
        this.catalogDao = catalogDao;
        this.chapterDao = chapterDao;
        this.comicsConfig = comicsConfig;
    }

    @PostConstruct
    @Scheduled(cron = "0 0 0 * * ?")
    public void init() {
        logger.info("comics data initializing... ");

        COMICS.putAll(Arrays.stream(comicsConfig.getComics().split(" "))
            .collect(Collectors.toMap(key -> key.split("=")[0], value -> value.split("=")[1])));

        // load comics and catalogs
        COMICS.forEach((name, url) -> {
            final Optional<Comic> comicOptional = comicDao.findByName(name);
            Comic comic = new Comic();
            try (WebClient webClient = getWebClient()) {
                if (!comicOptional.isPresent()) {
                    HtmlPage bookPage = getHtmlPage(webClient, url);
                    if (bookPage == null) {
                        return;
                    }
                    webClient.waitForBackgroundJavaScript(TIMEOUT);
                    Document book = Jsoup.parse(bookPage.asXml());
                    final Element coverImg = book.select("p.cover>img").first();
                    comic.setName(name);
                    comic.setUrl(url);
                    comic.setThumbnail(coverImg.attr("src"));
                    comic = comicDao.save(comic);
                } else {
                    comic = comicOptional.get();
                }
                HtmlPage catalogPage = getHtmlPage(webClient, comic.getUrl());
                if (catalogPage == null) {
                    return;
                }
                webClient.waitForBackgroundJavaScript(TIMEOUT);
                Document catalog = Jsoup.parse(catalogPage.asXml());
                Element ul = catalog.getElementById("chapter-list-1");
                List<Catalog> catalogs = catalogDao.findAllByComicId(comic.getId());
                Elements targetCatalogs = ul.children();
                if (CollectionUtils.isEmpty(catalogs)) {
                    Comic finalComic = comic;
                    List<Catalog> catalogList = targetCatalogs.stream().map(li -> {
                        Catalog temp = new Catalog();
                        temp.setName(li.text());
                        temp.setUrl(BASE_URL + li.select("a").first().attr("href"));
                        temp.setComicId(finalComic.getId());
                        return temp;
                    }).collect(Collectors.toList());
                    catalogDao.saveAll(catalogList);
                } else if (catalogs.size() < targetCatalogs.size()) {
                    for (int i = catalogs.size(); i < targetCatalogs.size(); i++) {
                        Element li = targetCatalogs.get(i);
                        Catalog temp = new Catalog();
                        temp.setName(li.text());
                        temp.setUrl(BASE_URL + li.select("a").first().attr("href"));
                        temp.setComicId(comic.getId());
                        catalogDao.save(temp);
                    }
                }
            }
        });

        logger.info("Catalogs initialize has finished... ");

        // load chapters

        COMICS.forEach((name, url) -> {
            final Runnable task = () -> {
                final Optional<Comic> comicOptional = comicDao.findByName(name);
                comicOptional.ifPresent(comic -> {
                    try (WebClient webClient = getWebClient()) {
                        List<Catalog> catalogs = catalogDao.findAllByComicId(comic.getId());
                        for (Catalog catalog : catalogs) {
                            List<Chapter> chapters = chapterDao.findAllByCatalogId(catalog.getId());
                            if (!CollectionUtils.isEmpty(chapters)) {
                                continue;
                            }
                            HtmlPage chapterPage = getHtmlPage(webClient, catalog.getUrl());
                            if (chapterPage == null) {
                                continue;
                            }
                            webClient.waitForBackgroundJavaScript(TIMEOUT);
                            Document chapter = Jsoup.parse(chapterPage.asXml());
                            Optional<Element> script = chapter.select("script").stream().filter(element -> {
                                String content = element.outerHtml();
                                return content.contains("CDATA") && content.contains("var chapterImages");
                            }).findFirst();
                            if (script.isPresent()) {
                                String[] chapterImages = StringUtils
                                    .trim(
                                        StringUtils.substringBetween(script.get().toString(), "chapterImages", ";")
                                            .replaceAll("=", "").replaceAll("\\[", "").replaceAll("]", "")
                                            .replaceAll("\"", ""))
                                    .split(",");
                                String chapterPath = StringUtils
                                    .trim(StringUtils.substringBetween(script.get().toString(), "chapterPath", ";")
                                        .replaceAll("=", "")
                                        .replaceAll("\"", ""));
                                List<Chapter> chapterList = new ArrayList<>(chapterImages.length);
                                for (String chapterImage : chapterImages) {
                                    Chapter tempChapter = new Chapter();
                                    tempChapter.setUrl(IMAGE_URL + chapterPath + chapterImage);
                                    tempChapter.setCatalogId(catalog.getId());
                                    chapterList.add(tempChapter);
                                }
                                chapterDao.saveAll(chapterList);
                            }
                        }
                    }
                    logger.info(String.format("《%s》 load finish", comic.getName()));
                });
            };
            new Thread(task).start();
        });
    }

    private HtmlPage getHtmlPage(WebClient webClient, String url) {
        HtmlPage page = null;
        String error = url + " connect error";
        IOException cause = null;
        for (int i = 0; i < 5; i++) {
            try {
                WebRequest webRequest = new WebRequest(new URL(url));
                Map<String, String> httpHeaders = new HashMap<>();
                httpHeaders.put("Connection", "Keep-Alive");
                webRequest.setAdditionalHeaders(httpHeaders);
                page = webClient.getPage(webRequest);
                break;
            } catch (IOException e) {
                cause = e;
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ignored) {
                }
            }
        }
        if (page == null) {
            logger.error(error, cause);
        }
        return page;
    }

    private static WebClient getWebClient() {
        WebClient webClient = new WebClient(BrowserVersion.CHROME);
        webClient.getOptions().setJavaScriptEnabled(true);
        webClient.getOptions().setCssEnabled(false);
        webClient.getOptions().setActiveXNative(false);
        webClient.getOptions().setCssEnabled(false);
        webClient.getOptions().setThrowExceptionOnScriptError(false);
        webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
        webClient.getOptions().setTimeout(5000);
        return webClient;
    }

    @RequestMapping("/")
    public String index(Model model) {
        List<Comic> allComics = comicDao.findAll();
        if (allComics.size() < COMICS.size()) {
            model.addAttribute("error", "还没读取完漫画列表，");
            return "index";
        }
        model.addAttribute("comics", allComics);
        return "index";
    }

    @RequestMapping("/update")
    public String updateIndex() {
        init();
        return "index";
    }


    @GetMapping("/book/{name}")
    public String list(@PathVariable String name, Model model) {
        Optional<Comic> comicOptional = comicDao.findByName(name);
        comicOptional.ifPresent(comic -> {
            model.addAttribute("current", comic);
            model.addAttribute("catalogs", catalogDao.findAllByComicId(comic.getId()));
            Optional<Catalog> catalogOptional = catalogDao.findByLastReadAndComicId(true, comic.getId());
            catalogOptional.ifPresent(catalog -> model.addAttribute("lastRead", catalog));
        });
        return "list";
    }

    @GetMapping("/chapter/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Optional<Catalog> catalog = catalogDao.findById(id);
        if (!catalog.isPresent()) {
            model.addAttribute("error", "没有此章节");
            return "chapter";
        }
        model.addAttribute("catalog", catalog.get());
        Optional<Catalog> next = catalogDao.findTopByIdAfterAndComicIdOrderByIdAsc(id, catalog.get().getComicId());
        Optional<Catalog> prev = catalogDao.findTopByIdBeforeAndComicIdOrderByIdDesc(id, catalog.get().getComicId());
        model.addAttribute("chapters", chapterDao.findAllByCatalogId(catalog.get().getId()));
        next.ifPresent(catalog1 -> model.addAttribute("next", catalog1));
        prev.ifPresent(catalog1 -> model.addAttribute("prev", catalog1));
        Optional<Comic> comicOptional = comicDao.findById(catalog.get().getComicId());
        comicOptional.ifPresent(comic -> model.addAttribute("current", comic.getName()));
        catalogDao.markLastRead(id);
        catalogDao.revertOtherLastRead(id, catalog.get().getComicId());
        return "chapter";
    }

}
