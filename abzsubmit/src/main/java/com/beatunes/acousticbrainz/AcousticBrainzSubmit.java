/*
 * =================================================
 * Copyright 2017 tagtraum industries incorporated
 * All rights reserved.
 * =================================================
 */
package com.beatunes.acousticbrainz;

import com.tagtraum.audiokern.AudioId;
import com.tagtraum.audiokern.AudioMetaData;
import com.tagtraum.audiokern.AudioSong;
import com.tagtraum.audiokern.StandardAudioId;
import com.tagtraum.beatunes.BeaTunes;
import com.tagtraum.beatunes.BeaTunesProperties;
import com.tagtraum.beatunes.analysis.AnalysisException;
import com.tagtraum.beatunes.analysis.AudioAnalysisTask;
import com.tagtraum.beatunes.analysis.Task;
import com.tagtraum.beatunes.messages.Message;
import com.tagtraum.beatunes.onlinedb.OnlineDB;
import com.tagtraum.core.FileId;
import com.tagtraum.core.FileUtilities;
import com.tagtraum.core.OperatingSystem;
import com.tagtraum.core.ProgressListener;
import org.jetbrains.annotations.NotNull;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.Entity;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.util.*;
import java.util.stream.Collectors;

import static java.nio.file.attribute.PosixFilePermission.*;

/**
 * AcousticBrainzSubmit.
 *
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 */
@Entity
public class AcousticBrainzSubmit extends AudioAnalysisTask {

    private static final Logger LOG = LoggerFactory.getLogger(AcousticBrainzSubmit.class);
    // SHA-1 to be used on submission profile
    private static final String ESSENTIA_BUILD_SHA = OperatingSystem.isMac() ? "cead25079874084f62182a551b7393616cd33d87" : "2d9f1f26377add8aeb1075a9c2973f962c4f09fd";
    private static final String STREAMING_EXTRACTOR_MUSIC = "streaming_extractor_music" + (OperatingSystem.isMac() ? "" : ".exe");
    private static final String PROFILE_YAML = "profile.yaml";
    private static final int OK = 0;
    private static final int THIRTY_MINUTES = 1000 * 60 * 30;
    private static Path executable;
    private static boolean hookRegistered;

    static {
        try {
            executable = extractBinary();
        } catch (Exception e) {
            LOG.error(e.toString(), e);
        }
    }

    public AcousticBrainzSubmit() {
        setProgressRelevant(true);
    }

    public String getName() {
        return "<html>AcousticBrainz<br>Submit</html>";
    }

    public String getDescription() {
        return "<h1>AcousticBrainz Submit</h1><p>Lets you execute the <a href=\"https://acousticbrainz.org\">AcousticBrainz</a> " +
            "feature extractor on your music files and submit the results to AcousticBrainz.</p>" +
            "<p>AcousticBrainz aims to crowd source acoustic information for all music in the world and to make it available " +
            "to the public. This acoustic information describes the acoustic characteristics of music and includes low-level " +
            "spectral information and information for genres, moods, keys, scales and much more. The goal of AcousticBrainz is " +
            "to provide music technology researchers and open source hackers with a massive database of information about music.</p>" +
            "<p>AcousticBrainz is a joint effort between <a href=\"http://www.mtg.upf.edu/\">Music Technology Group</a> " +
            "at Universitat Pompeu Fabra in Barcelona and the <a href=\"http://musicbrainz.org/\">MusicBrainz</a> project. It is " +
            "not affiliated with <a href=\"https://www.beatunes.com\">beaTunes</a> or <a href=\"http://www.tagtraum.com\">tagtraum industries</a>, " +
            "but some of the project's data is also used by beaTunes.</p>";
    }


    @Override
    public void setApplication(final BeaTunes beaTunes) {
        super.setApplication(beaTunes);
        registerShutdownHook();
    }

    private void registerShutdownHook() {
        synchronized (AcousticBrainzSubmit.class) {
            if (!hookRegistered) {
                getApplication().addShutdownHook(() -> {
                    final Path executableParent = executable.getParent();
                    if (LOG.isDebugEnabled()) LOG.debug("Deleting temporary AcousticBrainz binaries from " + executableParent);
                    try {
                        FileUtilities.deleteRecursively(executableParent);
                    } catch (Exception e) {
                        LOG.error("Failure while deleting temporary AcousticBrainz binaries " + executableParent, e);
                    }
                    return true;
                });
                hookRegistered = true;
            }
        }
    }

    @Override
    public void runBefore(final Task task) throws AnalysisException {
        final AudioSong song = getSong();
        if (song != null && song.getFile() != null) {
            // AC submit tends to crash for very long tracks and
            // the results aren't meaningful anyway, because of averaging.
            // Therefore we do not submit anything that's longer than 30min
            if (song.getTotalTime() >= THIRTY_MINUTES) {
                if (LOG.isDebugEnabled()) LOG.debug("Skipping track, because it is too long: " + song);
                return;
            }
            final List<Path> filesToDelete = new ArrayList<>();
            try {
                final String mbid = getMBID(song);
                if (mbid != null) {
                    process(song, mbid, filesToDelete);
                } else {
                    getMessagePanel().addMessage(new Message(
                        getApplication().localize("Analysis"),
                        "Failed to submit '" + song.getName() + "' to AcousticBrainz. Unable to find MusicBrainz ID.",
                        song.getId()
                    ));
                }
            } catch (Exception e) {
                LOG.error(e.toString(), e);
                getMessagePanel().addMessage(new Message(
                    getApplication().localize("Analysis"),
                    "Failed to submit '" + song.getName() + "' to AcousticBrainz: " + e,
                    song.getId()
                ));
            } finally {
                getAnalysisProgress().getOperationProgressListener().progress(1f);

                // cleanup
                filesToDelete.forEach(file -> {
                    try {
                        Files.deleteIfExists(file);
                    } catch (IOException e) {
                        LOG.error(e.toString(), e);
                    }
                });
            }
        } else {
            getMessagePanel().addMessage(new Message(
                getApplication().localize("Analysis"),
                "Failed to submit '" + song.getName() + "' to AcousticBrainz. File not found.",
                song.getId()
            ));
        }
    }

    private void process(final AudioSong song,
                         final String mbid,
                         final List<Path> deleteList) throws IOException, UnsupportedAudioFileException, InterruptedException, ParseException {
        final ProgressListener progressListener = getAnalysisProgress().getOperationProgressListener();
        progressListener.progress(0.25f);
        final Set<String> allMBIDs = getMBIDs(song);
        if (allMBIDs.size() > 1) {
            LOG.warn("Track " + song.getName() + ". Found multiple MBIDs: " + allMBIDs);
        } else {
            if (LOG.isDebugEnabled()) LOG.debug("Track " + song.getName() + ". Found MBID " + mbid);
        }
        // AudioMetaData is the direct access to the file, without going through
        // any indirection like the beaTunes internal database
        final Set<String> embeddedMBID = getMBIDs(song.getImplementation(AudioMetaData.class));
        final Path inputFile;
        if (embeddedMBID.isEmpty()) {
            if (LOG.isInfoEnabled()) LOG.info("Track " + song.getName() + ". MBID is not embedded. Embedding " + mbid + " into copy. Consider embedding MBIDs before running this task.");
            inputFile = createCopyWithMBID(song, mbid);
            deleteList.add(inputFile);
        } else {
            inputFile = song.getFile().toAbsolutePath();
        }
        progressListener.progress(0.4f);
        final Path outputFile = Files.createTempFile(executable.getParent(), "acousticbrainz", ".json").toAbsolutePath();
        deleteList.add(outputFile);
        final Process process = executeStreamingExtractorMusic(inputFile, outputFile);
        if (LOG.isDebugEnabled()) LOG.debug("Output: " + getOutput(process));
        final int exitCode = process.waitFor();
        progressListener.progress(0.5f);
        if (exitCode == OK) {
            final String usedMBID = extractMBID(mbid, outputFile);
            postToAcousticBrainz(song, usedMBID, outputFile);
        } else {
            LOG.error("Failed to analyze/submit " + song + ". Input file: " + inputFile + ". Exit code: " + exitCode);
            getMessagePanel().addMessage(new Message(
                getApplication().localize("Analysis"),
                "Failed to submit '" + song.getName() + "' to AcousticBrainz. Exit code " + exitCode + ". See log for details.",
                song.getId()
            ));
        }
    }

    private Set<String> getMBIDs(final AudioSong song) {
        return song.getTrackIds()
            .stream()
            .filter(id -> AudioId.MUSIC_BRAINZ_TRACK.equals(id.getGeneratorName()))
            .map(AudioId::getId)
            .map(String::toLowerCase)
            .collect(Collectors.toSet());
    }

    @NotNull
    private Path createCopyWithMBID(final AudioSong song, final String mbid) throws IOException, UnsupportedAudioFileException {
        final Path inputFile;// at this point, we don't want to manipulate the original file...
        inputFile = Files.createTempFile("copy", FileUtilities.getExtension(song.getFile()));
        Files.copy(song.getFile(), inputFile, StandardCopyOption.REPLACE_EXISTING);

        // provide fileId and attributes to work around dir access caching issue.
        final FileId fileId = new FileId(inputFile);
        final BasicFileAttributes basicFileAttributes = Files.readAttributes(inputFile, BasicFileAttributes.class);

        // embed mbid into the copy
        AudioMetaData.get(inputFile, basicFileAttributes, fileId, true)
            .getTrackIds().add(new StandardAudioId(AudioId.MUSIC_BRAINZ_TRACK, mbid));
        return inputFile;
    }

    /**
     * Extract MBID from JSON output, if available.
     *
     * @param mbid MBID we found in our local database
     * @param outputFile JSON file produced by the AcousticBrainz extractor
     * @return MBID that matched the JSON file
     * @throws IOException
     * @throws ParseException
     */
    private String extractMBID(final String mbid, final Path outputFile) throws IOException, ParseException {
        final String usedMBID;
        try (final BufferedReader in = Files.newBufferedReader(outputFile)) {
            final JSONObject json = (JSONObject)new JSONParser().parse(in);
            final JSONObject metadata = (JSONObject)json.get("metadata");
            final JSONObject tags = (JSONObject)metadata.get("tags");
            final JSONArray extractedMBIDs = (JSONArray)tags.get("musicbrainz_trackid");
            final String extractedMBID = extractedMBIDs != null && !extractedMBIDs.isEmpty()
                ? ((String)extractedMBIDs.get(0)).toLowerCase()
                : null;
            if (extractedMBID != null && !extractedMBID.equals(mbid)) {
                if (LOG.isInfoEnabled()) LOG.info("Replaced originally found MBID " + mbid + " with " + extractedMBID);
                usedMBID = extractedMBID;
            } else {
                usedMBID = mbid;
            }
        }
        return usedMBID;
    }

    @NotNull
    private String getOutput(final Process process) throws IOException {
        final InputStream inputStream = process.getInputStream();
        final StringBuilder sb = new StringBuilder();
        final byte[] buf = new byte[256];
        int count;
        while ((count = inputStream.read(buf)) != -1) {
            sb.append(new String(buf, 0, count, StandardCharsets.US_ASCII));
        }
        return sb.toString();
    }

    @NotNull
    private Process executeStreamingExtractorMusic(final Path inputFile, final Path outputFile) throws IOException {
        final ProcessBuilder builder = new ProcessBuilder(
            executable.toString(),
            inputFile.toString(),
            outputFile.toString(),
            executable.resolveSibling(PROFILE_YAML).toString());
        builder.redirectErrorStream(true);
        builder.directory(executable.getParent().toFile());
        return builder.start();
    }

    private void postToAcousticBrainz(final AudioSong song, final String mbid, final Path file) throws IOException {
        final URL url = new URL("https://acousticbrainz.org/api/v1/" + mbid.toLowerCase() + "/low-level");
        if (LOG.isDebugEnabled()) LOG.debug("Posting to " + url);
        final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setReadTimeout(10000);
        connection.setConnectTimeout(5000);
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("User-Agent", BeaTunesProperties.getInstance().getUserAgent());
        connection.setRequestProperty("Content-Encoding", "gzip");
        connection.setRequestProperty("Content-Type", "application/json");

        try (final OutputStream outputStream = new BufferedOutputStream(connection.getOutputStream())) {
            Files.copy(file, outputStream);
        }
        final int responseCode = connection.getResponseCode();
        if (responseCode != 200 && responseCode != 201) {
            final String message = readErrorMessage(connection);
            getMessagePanel().addMessage(new Message(
                getApplication().localize("Analysis"),
                "Failed to submit '" + song.getName() + "' to AcousticBrainz. " + responseCode + ": " + connection.getResponseMessage() + ". " + message,
                song.getId()
            ));
        }
    }

    private String readErrorMessage(final HttpURLConnection connection) throws IOException {
        final byte[] b = new byte[1024*4];
        final int read = connection.getErrorStream().read(b);
        String message = new String(b, 0, read, StandardCharsets.US_ASCII);
        LOG.error(message);
        if (message.contains("\"message\":")) {
            try {
                final JSONParser parser = new JSONParser();
                final JSONObject object = (JSONObject)parser.parse(message);
                message = (String)object.get("message");
            } catch (Exception e) {
                LOG.error(e.toString(), e);
            }
        }
        return message;
    }

    /**
     * Extract MBID from {@link AudioSong} object and if we cannot find it,
     * attempt to look it up in the central database.
     *
     * @param song song
     * @return MBID or null
     */
    private String getMBID(final AudioSong song) {
        return song.getTrackIds()
            .stream()
            .filter(id -> AudioId.MUSIC_BRAINZ_TRACK.equals(id.getGeneratorName()))
            .map(AudioId::getId)
            .map(String::toLowerCase)
            .findFirst().orElseGet(() -> {
                // there is no MBID embedded, let's look one up
                final OnlineDB onlineDB = getApplication().getPluginManager().getImplementation(OnlineDB.class);
                try {
                    return onlineDB.lookup(song)
                        .stream()
                        .flatMap(s -> s.getTrackIds().stream())
                        .filter(id -> AudioId.MUSIC_BRAINZ_TRACK.equals(id.getGeneratorName()))
                        .map(AudioId::getId)
                        .map(String::toLowerCase)
                        .findFirst().orElse(null);
                } catch (Exception e) {
                    LOG.error("Failed to look up MBID via OnlineDB.", e);
                }
                return null;
            });
    }

    private static Path extractBinary() throws IOException {
        if (LOG.isDebugEnabled()) LOG.debug("Extracting AcousticBrainz binaries...");
        try (final InputStream in = AcousticBrainzSubmit.class.getResourceAsStream(STREAMING_EXTRACTOR_MUSIC)) {
            final Path dir = Files.createTempDirectory("abzsubmit");
            if (LOG.isDebugEnabled()) LOG.debug("Executable directory: " + dir);
            final Path executable = dir.resolve(STREAMING_EXTRACTOR_MUSIC);
            Files.copy(in, executable);
            try {
                // actually make executable
                final Set<PosixFilePermission> permissions = new HashSet<>(Arrays.asList(
                    OWNER_READ, OWNER_WRITE, OWNER_EXECUTE,
                    GROUP_READ, GROUP_EXECUTE,
                    OTHERS_READ, OTHERS_EXECUTE
                ));
                Files.setPosixFilePermissions(executable, permissions);
            } catch (UnsupportedOperationException e) {
                LOG.warn("Was not able to make executable. Operation not supported on this platform.");
            }
            // create profile, see https://github.com/MTG/acousticbrainz-client/blob/master/abz/config.py#L60-L65
            try (final BufferedWriter writer = Files.newBufferedWriter(dir.resolve(PROFILE_YAML))) {
                writer.write("requireMbid: true\n" +
                    "indent: 0\n" +
                    "mergeValues:\n" +
                    "    metadata:\n" +
                    "        version:\n" +
                    "            essentia_build_sha: " + ESSENTIA_BUILD_SHA + "\n");
            }

            return executable.toAbsolutePath();
        }
    }
}
