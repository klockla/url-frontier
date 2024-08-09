package crawlercommons.urlfrontier.service;

import java.time.Instant;

import crawlercommons.urlfrontier.Urlfrontier.AckMessage.Status;
import crawlercommons.urlfrontier.Urlfrontier.DiscoveredURLItem;
import crawlercommons.urlfrontier.Urlfrontier.KnownURLItem;
import crawlercommons.urlfrontier.Urlfrontier.StringList;
import crawlercommons.urlfrontier.Urlfrontier.URLInfo;
import crawlercommons.urlfrontier.Urlfrontier.URLItem;

public class ServiceTestUtil {

    public static void initURLs(AbstractFrontierService service) {

        String crawlId = "crawl_id";
        String url1 = "https://www.mysite.com/discovered";
        String key1 = "queue_mysite";
        String url2 = "https://www.mysite.com/completed";
        String key2 = "queue_mysite";
        String url3 = "https://www.mysite.com/knowntorefetch";
        String key3 = "queue_mysite";


        int sent = 0;

        crawlercommons.urlfrontier.Urlfrontier.URLItem.Builder ibuilder = URLItem.newBuilder();
        StringList sl1 = StringList.newBuilder().addValues("md1").build();
        StringList sl2 = StringList.newBuilder().addValues("md2").build();
        StringList sl3 = StringList.newBuilder().addValues("md3").build();

        URLInfo info1 =
                URLInfo.newBuilder()
                        .setUrl(url1)
                        .setCrawlID(crawlId)
                        .setKey(key1)
                        .putMetadata("meta1", sl1)
                        .build();

        DiscoveredURLItem disco1 = DiscoveredURLItem.newBuilder().setInfo(info1).build();
        ibuilder.setDiscovered(disco1);
        ibuilder.setID(crawlId + "_" + url1);

        Status s = service.putURLItem(ibuilder.build());
        sent++;

        URLInfo info2 =
                URLInfo.newBuilder()
                        .setUrl(url2)
                        .setCrawlID(crawlId)
                        .setKey(key2)
                        .putMetadata("meta1", sl2)
                        .build();

        KnownURLItem known =
                KnownURLItem.newBuilder().setInfo(info2).setRefetchableFromDate(0).build();

        ibuilder.clear();
        ibuilder.setKnown(known);
        ibuilder.setID(crawlId + "_" + url2);

        s = service.putURLItem(ibuilder.build());
        sent++;

        URLInfo info3 =
                URLInfo.newBuilder()
                        .setUrl(url3)
                        .setCrawlID(crawlId)
                        .setKey(key3)
                        .putMetadata("meta3", sl3)
                        .build();

        KnownURLItem torefetch =
                KnownURLItem.newBuilder()
                        .setInfo(info3)
                        .setRefetchableFromDate(Instant.now().getEpochSecond() + 3600)
                        .build();

        ibuilder.clear();
        ibuilder.setKnown(torefetch);
        ibuilder.setID(crawlId + "_" + url3);

        s = service.putURLItem(ibuilder.build());
        sent++;
    }
}
