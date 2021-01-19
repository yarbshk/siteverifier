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

    private static final int ACTION_THRESHOLD = 3;

    public SiteVerifierApp(List<String> inSites, int start, int length, Set<String> outSites)
    {
        this.inSites = inSites;
        this.start = start;
        this.length = length;
        this.outSites = outSites;
    }

    public static void main(String[] args) throws Exception
    {
        if (args.length != 2)
        {
            System.out.println("Usage: java -jar app.jar in-sites.txt out-sites.txt");
            System.exit(1);
            return;
        }

        Path inSitesPath = Path.of(args[0]);
        Path outSitesPath = Path.of(args[1]);

        List<String> inSites = Collections.unmodifiableList(Files.readAllLines(inSitesPath));
        Set<String> outSites = new HashSet<>();
        ForkJoinPool pool = new ForkJoinPool();
        pool.invoke(new SiteVerifierApp(inSites, 0, inSites.size(), outSites));

        Files.write(outSitesPath, outSites);
    }

    @Override
    protected void compute()
    {
        if (length < ACTION_THRESHOLD)
        {
            computeDirectly();
            return;
        }

        int split = length / 2;
        invokeAll(
                new SiteVerifierApp(inSites, start, split, outSites),
                new SiteVerifierApp(inSites, start + split, length - split, outSites));
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
