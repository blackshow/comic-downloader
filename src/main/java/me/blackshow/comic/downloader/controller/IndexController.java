package me.blackshow.comic.downloader.controller;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import me.blackshow.comic.downloader.entity.Comic;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class IndexController {

    private static final Map<String, String> COMICS = new LinkedHashMap<>();
    private static final List<Comic> COMIC_LIST = new ArrayList<>();

    private static final String BASE_URL = "http://www.gufengmh.com/";

    {
        COMICS.put("我为苍生", "http://www.gufengmh.com/manhua/woweicangsheng/");
        COMICS.put("戒魔人", "http://www.gufengmh.com/manhua/jiemoren/");
        COMICS.put("绝品透视", "http://www.gufengmh.com/manhua/juepintoushi/");
        COMICS.put("极道天使", "http://www.gufengmh.com/manhua/jidaotianshi/");
        COMICS.put("我的天劫女友", "http://www.gufengmh.com/manhua/wodetianjienvyou/");
        COMICS.put("红雾", "http://www.gufengmh.com/manhua/hongwu/");
        COMICS.put("花悸", "http://www.gufengmh.com/manhua/huaji/");
        COMICS.put("演平乱志", "http://www.gufengmh.com/manhua/yanpingluanzhi/");
        for (Entry<String, String> entry : COMICS.entrySet()) {
            final String name = entry.getKey();
            final String url = entry.getValue();
            final Runnable task = () -> {
                try (WebClient webClient = getWebClient()) {
                    HtmlPage bookPage = webClient.getPage(url);
                    webClient.waitForBackgroundJavaScript(5000);
                    Document book = Jsoup.parse(bookPage.asXml());
                    final Element coverImg = book.select("p.cover>img").first();
                    final Comic comic = new Comic();
                    comic.setName(name);
                    comic.setUrl(url);
                    comic.setThumbnail(coverImg.attr("src"));
                    COMIC_LIST.add(comic);
                } catch (IOException ignored) {
                }
            };
            new Thread(task).start();
        }
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
        if (COMIC_LIST.size() < COMICS.size()) {
            model.addAttribute("error", "还没读取完漫画列表，");
        }
        model.addAttribute("comics", COMIC_LIST);
        return "index";
    }

    @GetMapping("/book/{name}")
    public String list(@PathVariable String name, Model model) {
        final String url = COMICS.get(name);
        List<Comic> list = new ArrayList<>();
        try (WebClient webClient = getWebClient()) {
            HtmlPage bookPage = webClient.getPage(url);
            webClient.waitForBackgroundJavaScript(5000);
            Document book = Jsoup.parse(bookPage.asXml());
            Element ul = book.getElementById("chapter-list-1");
            for (int i = 0; i < ul.children().size(); i++) {
                Element li = ul.child(i);
                final Comic comic = new Comic();
                comic.setName(li.text());
                comic.setUrl(BASE_URL + li.select("a").first().attr("href"));
                list.add(comic);
            }
        } catch (IOException ignored) {
        }
        model.addAttribute("comics", list);
        return "list";
    }

}
