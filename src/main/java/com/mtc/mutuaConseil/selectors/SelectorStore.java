package com.mtc.mutuaConseil.selectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Charge les sélecteurs (XPath/CSS/id/texte) des sites fournisseurs depuis des fichiers
 * YAML externes ({@code selectors.base-path}/&lt;provider&gt;.yml), en dehors du jar/war.
 * Quand un site change, on édite le YAML -- pas besoin de recompiler ni de redéployer.
 * Le fichier est rechargé automatiquement dès que sa date de modification change.
 */
@Component
public class SelectorStore {

    private static final Logger log = LoggerFactory.getLogger(SelectorStore.class);

    @Value("${selectors.base-path:selectors}")
    private String basePath;

    private final Map<String, LoadedFile> cache = new ConcurrentHashMap<>();

    private record LoadedFile(Map<String, String> values, long lastModifiedMillis) {
    }

    public String get(String provider, String key) {
        Map<String, String> values = load(provider);
        String value = values.get(key);
        if (value == null) {
            throw new SelectorNotFoundException(provider, key, resolvePath(provider));
        }
        return value;
    }

    private Map<String, String> load(String provider) {
        Path path = resolvePath(provider);
        long lastModified;
        try {
            lastModified = Files.getLastModifiedTime(path).toMillis();
        } catch (IOException e) {
            throw new SelectorNotFoundException(provider, path, e);
        }

        LoadedFile cached = cache.get(provider);
        if (cached != null && cached.lastModifiedMillis() == lastModified) {
            return cached.values();
        }

        Map<String, String> flat = new LinkedHashMap<>();
        try (InputStream in = Files.newInputStream(path)) {
            Object data = new Yaml().load(in);
            flatten("", data, flat);
        } catch (IOException e) {
            throw new SelectorNotFoundException(provider, path, e);
        }

        cache.put(provider, new LoadedFile(flat, lastModified));
        log.info("Sélecteurs chargés pour '{}' depuis {} ({} entrées)", provider, path, flat.size());
        return flat;
    }

    @SuppressWarnings("unchecked")
    private void flatten(String prefix, Object node, Map<String, String> out) {
        if (node instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = prefix.isEmpty() ? entry.getKey().toString() : prefix + "." + entry.getKey();
                flatten(key, entry.getValue(), out);
            }
        } else if (node != null) {
            out.put(prefix, node.toString());
        }
    }

    private Path resolvePath(String provider) {
        return Paths.get(basePath, provider + ".yml");
    }
}
