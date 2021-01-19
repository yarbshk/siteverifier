package org.example.siteverifier;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

public class SiteVerifierApp extends RecursiveAction
{
    private final List<String> inSites;
    private final int start;
    private final int length;
    private final Set<String> outSites;
    private final int threshold;

    public SiteVerifierApp(List<String> inSites, int start, int length, Set<String> outSites, int threshold)
    {
        this.inSites = inSites;
        this.start = start;
        this.length = length;
        this.outSites = outSites;
        this.threshold = threshold;
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
        pool.invoke(new SiteVerifierApp(inSites, 0, inSites.size(), outSites, threshold));

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
                new SiteVerifierApp(inSites, start, split, outSites, threshold),
                new SiteVerifierApp(inSites, start + split, length - split, outSites, threshold));
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
                System.out.println("Invalid URL: " + rawUrl);
                continue;
            }

            URLConnection urlConnection;
            try
            {
                urlConnection = url.openConnection();
            }
            catch (IOException e)
            {
                System.out.println("Unable to open connection with " + rawUrl);
                continue;
            }

            try
            {
                urlConnection.connect();
            }
            catch (IOException e)
            {
                System.out.println("Unable to connect to " + rawUrl);
                continue;
            }

            outSites.add(rawUrl);
        }
    }
}
