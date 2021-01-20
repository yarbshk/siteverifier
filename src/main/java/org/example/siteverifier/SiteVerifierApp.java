package org.example.siteverifier;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

public class SiteVerifierApp extends RecursiveAction
{
    private static final int DEFAULT_TIMEOUT = 5 * 1000; // 5 sec

    private final List<String> inSites;
    private final int start;
    private final int length;
    private final Set<String> outSites;
    private final int threshold;
    private final int timeoutMillis;

    public SiteVerifierApp(List<String> inSites, int start, int length, Set<String> outSites, int threshold, int timeoutMillis)
    {
        this.inSites = inSites;
        this.start = start;
        this.length = length;
        this.outSites = outSites;
        this.threshold = threshold;
        this.timeoutMillis = timeoutMillis;
    }

    public static void main(String[] args) throws Exception
    {
        if (args.length != 3)
        {
            System.out.println("Usage: <INPUT_FILE> <OUTPUT_FILE> <THRESHOLD>");
            System.exit(1);
            return;
        }

        Path inSitesPath = Path.of(args[0]);
        Path outSitesPath = Path.of(args[1]);
        int threshold = Integer.parseInt(args[2]);

        List<String> inSites = Collections.unmodifiableList(Files.readAllLines(inSitesPath));
        Set<String> outSites = new HashSet<>();
        ForkJoinPool pool = new ForkJoinPool();
        pool.invoke(new SiteVerifierApp(inSites, 0, inSites.size(), outSites, threshold, DEFAULT_TIMEOUT));

        Files.write(outSitesPath, outSites);
    }

    @Override
    protected void compute()
    {
        if (length < threshold)
        {
            computeDirectly();
            return;
        }

        int split = length / 2;
        invokeAll(
                new SiteVerifierApp(inSites, start, split, outSites, threshold, timeoutMillis),
                new SiteVerifierApp(inSites, start + split, length - split, outSites, threshold, timeoutMillis));
    }

    private void computeDirectly()
    {
        for (int i = start; i < start + length; i++)
        {
            String rawUrl = inSites.get(i);
            URL url;
            try
            {
                url = new URL(rawUrl);
            }
            catch (MalformedURLException e)
            {
                System.out.println("Error! Invalid URL - " + rawUrl);
                continue;
            }

            URLConnection urlConnection;
            try
            {
                urlConnection = url.openConnection();
            }
            catch (IOException e)
            {
                System.out.println("Error! Unable to open connection with " + rawUrl);
                continue;
            }

            urlConnection.setReadTimeout(timeoutMillis);
            urlConnection.setConnectTimeout(timeoutMillis);
            try
            {
                urlConnection.connect();
            }
            catch (IOException e)
            {
                System.out.println("Error! Unable to connect to " + rawUrl);
                continue;
            }

            if (urlConnection.getContentLength() < 0)
            {
                System.err.println("Error! No content at " + rawUrl);
                continue;
            }

            InputStream inputStream;
            try
            {
                inputStream = urlConnection.getInputStream();
            }
            catch (IOException e)
            {
                System.out.println("Error! Unable to read page content");
                continue;
            }

            boolean dummyFlag = false;
            try (Scanner scanner = new Scanner(inputStream).useDelimiter("\\A"))
            {
                while (scanner.hasNext())
                {
                    if (scanner.next().contains("error"))
                    {
                        dummyFlag = true;
                        break;
                    }
                }
            }

            if (!dummyFlag)
            {
                System.out.println("Error! Unsupported content");
                continue;
            }

            outSites.add(rawUrl);
            System.out.println("Success! Verified URL - " + rawUrl);
        }

        System.out.printf("\n***** TOTAL NUMBER OF VERIFIED URLS: %s *****\n\n", outSites.size());
    }
}
