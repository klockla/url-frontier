package crawlercommons.urlfrontier.service;

import crawlercommons.urlfrontier.URLFrontierGrpc;
import crawlercommons.urlfrontier.URLFrontierGrpc.URLFrontierBlockingStub;
import crawlercommons.urlfrontier.URLFrontierGrpc.URLFrontierStub;
import crawlercommons.urlfrontier.Urlfrontier.DiscoveredURLItem;
import crawlercommons.urlfrontier.Urlfrontier.GetParams;
import crawlercommons.urlfrontier.Urlfrontier.ListUrlParams;
import crawlercommons.urlfrontier.Urlfrontier.URLInfo;
import crawlercommons.urlfrontier.Urlfrontier.URLItem;
import crawlercommons.urlfrontier.Urlfrontier.URLStatusRequest;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class StressTest {

    private static final String SERVER_ADDRESS = "localhost";
    private static final int NUM_CLIENTS = 50;
    private static final int NUM_REQUESTS_PER_CLIENT = 10;
    private static final long REQUEST_INTERVAL_MS = 10;

    public static void main(String[] args) {
        ManagedChannel channel =
                ManagedChannelBuilder.forAddress(SERVER_ADDRESS, 7071).usePlaintext().build();

        ExecutorService executorService = Executors.newFixedThreadPool(NUM_CLIENTS);

        long start = System.currentTimeMillis();
        for (int i = 0; i < NUM_CLIENTS; i++) {
            int clientId = i;
            executorService.submit(() -> runClient(channel, clientId));
        }

        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(2, TimeUnit.HOURS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }

        long end = System.currentTimeMillis();
        long secs = (end - start) / 1000;
        System.out.println("Total exec time=" + secs);
    }

    private static void runClient(ManagedChannel channel, int clientId) {

        URLFrontierStub frontier = URLFrontierGrpc.newStub(channel);
        URLFrontierBlockingStub blockingFrontier = URLFrontierGrpc.newBlockingStub(channel);

        Random random = new Random();
        List<URLItem> lurl = null;

        for (int i = 0; i < NUM_REQUESTS_PER_CLIENT; i++) {
            lurl = new ArrayList<>();
            try {
                String crawlId = "1451502";

                // request to listURLs
                if (i % 2 == 0) {
                    crawlId = "1450901";
                }
                /*
                                Urlfrontier.Long count =
                                        blockingFrontier.countURLs(
                                                CountUrlParams.newBuilder().setCrawlID(crawlId).build());
                                System.out.println("Client " + clientId + ": CountURLs: " + count.getValue());
                */

                Iterator<URLItem> it =
                        blockingFrontier.listURLs(
                                ListUrlParams.newBuilder()
                                        .setCrawlID(crawlId)
                                        .setStart(0)
                                        // .setSize(Integer.MAX_VALUE)
                                        .setSize(100)
                                        .build());

                while (it.hasNext()) {
                    URLItem url = it.next();
                    // System.out.println(url.getID() + ";" + url.getKnown().getInfo().getUrl());
                    lurl.add(url);
                }

                // Simulate a request to getURLStatus
                if (!lurl.isEmpty()) {
                    int n = random.nextInt(lurl.size());

                    URLStatusRequest statusRequest =
                            URLStatusRequest.newBuilder()
                                    .setKey(lurl.get(n).getKnown().getInfo().getKey())
                                    .setCrawlID(crawlId)
                                    .setUrl(lurl.get(n).getKnown().getInfo().getUrl())
                                    .build();
                    URLItem response = blockingFrontier.getURLStatus(statusRequest);
                    System.out.println(
                            "Client "
                                    + clientId
                                    + ": URL status: "
                                    + response.getKnown().getInfo().getUrl());
                }

                // Send new URLs
                GetParams request3 =
                        GetParams.newBuilder()
                                .setKey("key1.com")
                                .setMaxUrlsPerQueue(1)
                                .setDelayRequestable(30)
                                .setCrawlID("1450901")
                                .build();

                Iterator<URLInfo> iinfo2 = blockingFrontier.getURLs(request3);
                if (iinfo2.hasNext()) {
                    System.out.println(
                            "Client " + clientId + ": GetURL: " + iinfo2.next().getUrl());
                } else {
                    URLInfo info =
                            URLInfo.newBuilder()
                                    .setUrl("http://key.com/client" + clientId + "_req" + i)
                                    .setKey("key.com")
                                    .setCrawlID(crawlId)
                                    .build();
                    DiscoveredURLItem item = DiscoveredURLItem.newBuilder().setInfo(info).build();

                    sendURLs(URLItem.newBuilder().setDiscovered(item).build());
                }

                // Call GetURL
                // want just one URL for that specific key, in the default crawl
                GetParams request2 =
                        GetParams.newBuilder()
                                // .setKey("key.com")
                                .setMaxUrlsPerQueue(1)
                                .setDelayRequestable(30)
                                .setCrawlID(crawlId)
                                .build();

                Iterator<URLInfo> iinfo = blockingFrontier.getURLs(request2);
                if (iinfo.hasNext()) {
                    System.out.println("Client " + clientId + ": GetURL: " + iinfo.next().getUrl());
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

            try {
                Thread.sleep(REQUEST_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static int sendURLs(URLItem... items) {
        final AtomicBoolean completed = new AtomicBoolean(false);
        final AtomicInteger acked = new AtomicInteger(0);

        ManagedChannel channel =
                ManagedChannelBuilder.forAddress(SERVER_ADDRESS, 7071).usePlaintext().build();

        StreamObserver<crawlercommons.urlfrontier.Urlfrontier.AckMessage> responseObserver =
                new StreamObserver<>() {

                    @Override
                    public void onNext(crawlercommons.urlfrontier.Urlfrontier.AckMessage value) {
                        acked.addAndGet(1);
                    }

                    @Override
                    public void onError(Throwable t) {
                        completed.set(true);
                        System.err.println("Error received" + t.getMessage());
                    }

                    @Override
                    public void onCompleted() {
                        completed.set(true);
                    }
                };

        URLFrontierStub frontier = URLFrontierGrpc.newStub(channel);
        StreamObserver<URLItem> streamObserver = frontier.putURLs(responseObserver);

        for (URLItem item : items) {
            streamObserver.onNext(item);
            System.out.println("Sending URL: " + item);
        }

        streamObserver.onCompleted();

        // wait for completion
        while (completed.get() == false) {
            try {
                Thread.currentThread().sleep(10);
            } catch (InterruptedException e) {
            }
        }

        channel.shutdown();

        return acked.get();
    }
}
