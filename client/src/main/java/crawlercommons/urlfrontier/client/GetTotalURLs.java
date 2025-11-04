// SPDX-FileCopyrightText: 2020 Crawler-commons
// SPDX-License-Identifier: Apache-2

package crawlercommons.urlfrontier.client;

import crawlercommons.urlfrontier.URLFrontierGrpc;
import crawlercommons.urlfrontier.URLFrontierGrpc.URLFrontierBlockingStub;
import crawlercommons.urlfrontier.Urlfrontier;
import crawlercommons.urlfrontier.Urlfrontier.Long;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

@Command(
        name = "GetTotalURLs",
        description = "Prints out the total number of URLs for a given crawl")
public class GetTotalURLs implements Runnable {

    @ParentCommand private Client parent;

    @Option(
            names = {"-c", "--crawlID"},
            required = true,
            defaultValue = "DEFAULT",
            paramLabel = "STRING",
            description = "crawl to get the total URL count for")
    private String crawl;

    @Override
    public void run() {
        ManagedChannel channel =
                ManagedChannelBuilder.forAddress(parent.hostname, parent.port)
                        .usePlaintext()
                        .build();

        URLFrontierBlockingStub blockingFrontier = URLFrontierGrpc.newBlockingStub(channel);

        Urlfrontier.GetTotalURLParams.Builder builder = Urlfrontier.GetTotalURLParams.newBuilder();

        builder.setCrawlID(crawl);

        Long s = blockingFrontier.getTotalURLs(builder.build());
        System.out.println(s.getValue() + " URLs in frontier");

        channel.shutdownNow();
    }
}
