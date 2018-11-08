package me.blackshow.comic.downloader.controller;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import java.io.IOException;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class IndexController {

    private static final Map<String, String> COMICS = new HashMap<>();
    private static final String BASE_URL = "http://www.gufengmh.com";
    private static final String IMAGE_URL = "http://res.gufengmh.com/";
    private static final int TIMEOUT = 1000;

    private final ComicDao comicDao;

    private final CatalogDao catalogDao;

    private final ChapterDao chapterDao;

    private final ComicsConfig comicsConfig;

    static {
//        COMICS.put("我为苍生", "http://www.gufengmh.com/manhua/woweicangsheng/");
//        COMICS.put("戒魔人", "http://www.gufengmh.com/manhua/jiemoren/");
//        COMICS.put("绝品透视", "http://www.gufengmh.com/manhua/juepintoushi/");
//        COMICS.put("极道天使", "http://www.gufengmh.com/manhua/jidaotianshi/");
//        COMICS.put("我的天劫女友", "http://www.gufengmh.com/manhua/wodetianjienvyou/");
//        COMICS.put("红雾", "http://www.gufengmh.com/manhua/hongwu/");
//        COMICS.put("花悸", "http://www.gufengmh.com/manhua/huaji/");
//        COMICS.put("演平乱志", "http://www.gufengmh.com/manhua/yanpingluanzhi/");
    }

    @Autowired
    public IndexController(ComicDao comicDao, CatalogDao catalogDao, ChapterDao chapterDao,
        ComicsConfig comicsConfig) {
        this.comicDao = comicDao;
        this.catalogDao = catalogDao;
        this.chapterDao = chapterDao;
        this.comicsConfig = comicsConfig;
    }

    @PostConstruct
    public void init() {
        COMICS.putAll(Arrays.stream(comicsConfig.getComics().split(" "))
            .collect(Collectors.toMap(key -> key.split("=")[0], value -> value.split("=")[1])));
        COMICS.forEach((name, url) -> {
            final Runnable task = () -> {
                final Optional<Comic> comicOptional = comicDao.findByName(name);
                if (comicOptional.isPresent()) {
                    return;
                }
                try (WebClient webClient = getWebClient()) {
                    HtmlPage bookPage = webClient.getPage(url);
                    webClient.waitForBackgroundJavaScript(TIMEOUT);
                    Document book = Jsoup.parse(bookPage.asXml());
                    final Element coverImg = book.select("p.cover>img").first();
                    Comic comic = comicOptional.orElse(new Comic());
                    comic.setName(name);
                    comic.setUrl(url);
                    comic.setThumbnail(coverImg.attr("src"));
                    comic = comicDao.save(comic);
                    HtmlPage catalogPage = webClient.getPage(comic.getUrl());
                    webClient.waitForBackgroundJavaScript(TIMEOUT);
                    Document catalog = Jsoup.parse(catalogPage.asXml());
                    Element ul = catalog.getElementById("chapter-list-1");
                    List<Catalog> catalogs = catalogDao.findAllByComicId(comic.getId());
                    if (CollectionUtils.isEmpty(catalogs) || catalogs.size() != ul.children().size()) {
                        for (Element li : ul.children()) {
                            Catalog temp = new Catalog();
                            temp.setName(li.text());
                            temp.setUrl(BASE_URL + li.select("a").first().attr("href"));
                            temp.setComicId(comic.getId());
                            temp = catalogDao.save(temp);
                            HtmlPage chapterPage = webClient.getPage(temp.getUrl());
                            webClient.waitForBackgroundJavaScript(TIMEOUT);
                            Document chapter = Jsoup.parse(chapterPage.asXml());
                            Optional<Element> script = chapter.select("script").stream().filter(element -> {
                                String content = element.outerHtml();
                                return content.contains("CDATA") && content.contains("var chapterImages");
                            }).findFirst();
                            if (script.isPresent()) {
                                String[] chapterImages = StringUtils
                                    .trim(StringUtils.substringBetween(script.get().toString(), "chapterImages", ";")
                                        .replaceAll("=", "").replaceAll("\\[", "").replaceAll("]", "")
                                        .replaceAll("\"", ""))
                                    .split(",");
                                String chapterPath = StringUtils
                                    .trim(StringUtils.substringBetween(script.get().toString(), "chapterPath", ";")
                                        .replaceAll("=", "")
                                        .replaceAll("\"", ""));
                                for (String chapterImage : chapterImages) {
                                    Chapter tempChapter = new Chapter();
                                    tempChapter.setUrl(IMAGE_URL + chapterPath + chapterImage);
                                    tempChapter.setCatalogId(temp.getId());
                                    chapterDao.save(tempChapter);
                                }

                            }

                        }
                    }
                } catch (IOException ignored) {
                }
            };
            new Thread(task).start();
        });
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
        System.out.println(comicsConfig);
        if (allComics.size() < COMICS.size()) {
            model.addAttribute("error", "还没读取完漫画列表，");
            return "index";
        }
        model.addAttribute("comics", allComics);
        return "index";
    }

    @GetMapping("/book/{name}")
    public String list(@PathVariable String name, Model model) {
        Optional<Comic> comicOptional = comicDao.findByName(name);
        comicOptional.ifPresent(comic -> model.addAttribute("catalogs", catalogDao.findAllByComicId(comic.getId())));
        return "list";
    }

    @GetMapping("/chapter/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Optional<Catalog> catalog = catalogDao.findById(id);
        if (!catalog.isPresent()) {
            model.addAttribute("error", "没有此章节");
            return "chapter";
        }
        Optional<Catalog> next = catalogDao.findTopByIdAfterAndComicIdOrderByIdAsc(id, catalog.get().getComicId());
        Optional<Catalog> prev = catalogDao.findTopByIdBeforeAndComicIdOrderByIdDesc(id, catalog.get().getComicId());
//        List<Chapter> chapters = new ArrayList<>();
//        try (WebClient webClient = getWebClient()) {
//            HtmlPage chapterPage = webClient.getPage(catalog.get().getUrl());
//            webClient.waitForBackgroundJavaScript(5000);
//            Document chapter = Jsoup.parse(chapterPage.asXml());
//            Element script = chapter.select("script").stream().filter(element -> {
//                String content = element.outerHtml();
//                return content.contains("CDATA") && content.contains("var chapterImages");
//            }).findFirst().get();
//            String[] chapterImages = StringUtils
//                .trim(StringUtils.substringBetween(script.toString(), "chapterImages", ";")
//                    .replaceAll("=", "").replaceAll("\\[", "").replaceAll("]", "").replaceAll("\"", "")).split(",");
//            String chapterPath = StringUtils
//                .trim(StringUtils.substringBetween(script.toString(), "chapterPath", ";").replaceAll("=", "")
//                    .replaceAll("\"", ""));
//            for (String chapterImage : chapterImages) {
//                Chapter temp = new Chapter();
//                temp.setUrl(IMAGE_URL + chapterPath + chapterImage);
//                temp.setCatalogId(id);
//                chapters.add(temp);
//            }
//            model.addAttribute("chapters", chapters);
//        } catch (IOException ignored) {
//        }

        model.addAttribute("chapters", chapterDao.findAllByCatalogId(catalog.get().getId()));
        next.ifPresent(catalog1 -> model.addAttribute("next", catalog1));
        prev.ifPresent(catalog1 -> model.addAttribute("prev", catalog1));
        catalogDao.markLastRead(id);
        catalogDao.revertOtherLastRead(id, catalog.get().getComicId());
        return "chapter";
    }

}
