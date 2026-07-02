package com.mtc.mutuaConseil.base;

import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.util.List;
import java.util.Random;

public final class FingerprintFactory {

    private static final Random RANDOM = new Random();

    private static final String LOCALE = "fr-FR";
    private static final String TIMEZONE = "Europe/Paris";
    private static final double PARIS_LATITUDE = 48.8566;
    private static final double PARIS_LONGITUDE = 2.3522;
    private static final double GEO_JITTER = 0.05;

    private record Template(BrowserType browserType, String name, String userAgent, int cpuCores, int memoryGb) { }

    private static final List<Template> TEMPLATES = List.of(
            new Template(BrowserType.PLAYWRIGHT_CHROMIUM, "Windows_Chromium",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36",
                    8, 16),
            new Template(BrowserType.PLAYWRIGHT_CHROME, "Windows_Chrome",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36",
                    8, 16),
            new Template(BrowserType.PLAYWRIGHT_CHROME, "MacOS_Chrome",
                    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36",
                    8, 16),
            new Template(BrowserType.PLAYWRIGHT_EDGE, "Windows_Edge",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36 Edg/126.0.0.0",
                    8, 16),
            new Template(BrowserType.PLAYWRIGHT_FIREFOX, "Windows_Firefox",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:128.0) Gecko/20100101 Firefox/128.0",
                    8, 16)
    );

    private FingerprintFactory() { }

    public static BrowserProfile randomProfile(BrowserType browserType) {
        List<Template> matching = TEMPLATES.stream()
                .filter(t -> t.browserType() == browserType)
                .toList();
        if (matching.isEmpty()) {
            matching = TEMPLATES.stream()
                    .filter(t -> t.browserType() == BrowserType.PLAYWRIGHT_CHROMIUM)
                    .toList();
        }
        return buildProfile(matching.get(RANDOM.nextInt(matching.size())));
    }

    private static BrowserProfile buildProfile(Template template) {
        Dimension screen = maxScreenSize();
        double latitude = PARIS_LATITUDE + (RANDOM.nextDouble() - 0.5) * GEO_JITTER;
        double longitude = PARIS_LONGITUDE + (RANDOM.nextDouble() - 0.5) * GEO_JITTER;

        return new BrowserProfile(
                template.name(),
                template.userAgent(),
                LOCALE,
                TIMEZONE,
                screen.width,
                screen.height,
                latitude,
                longitude,
                template.cpuCores(),
                template.memoryGb()
        );
    }

    private static Dimension maxScreenSize() {
        try {
            if (!GraphicsEnvironment.isHeadless()) {
                return Toolkit.getDefaultToolkit().getScreenSize();
            }
        } catch (HeadlessException ignored) {
            // pas d'affichage disponible (ex: exécution en conteneur headless)
        }
        return new Dimension(1920, 1080);
    }

    // Injecté via BrowserContext.addInitScript avant tout script de la page ciblée,
    // pour effacer les traces d'automatisation les plus contrôlées par les anti-bots
    public static String stealthScript(BrowserProfile profile) {
        return """
                Object.defineProperty(navigator, 'webdriver', { get: () => undefined });

                window.chrome = window.chrome || { runtime: {} };

                const originalQuery = window.navigator.permissions.query;
                window.navigator.permissions.query = (parameters) => (
                    parameters.name === 'notifications'
                        ? Promise.resolve({ state: Notification.permission })
                        : originalQuery(parameters)
                );

                Object.defineProperty(navigator, 'languages', { get: () => ['fr-FR', 'fr'] });
                Object.defineProperty(navigator, 'plugins', { get: () => [1, 2, 3, 4, 5] });
                Object.defineProperty(navigator, 'hardwareConcurrency', { get: () => %d });
                Object.defineProperty(navigator, 'deviceMemory', { get: () => %d });
                """.formatted(profile.cpuCores(), profile.memoryGb());
    }
}
