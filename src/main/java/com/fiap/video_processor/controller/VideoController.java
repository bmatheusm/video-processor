package com.fiap.video_processor.controller;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
@RequestMapping
public class VideoController {

    private final Path uploadsDir = Paths.get("uploads");
    private final Path outputsDir = Paths.get("outputs");
    private final Path tempDir = Paths.get("temp");

    public VideoController() throws IOException {
        Files.createDirectories(uploadsDir);
        Files.createDirectories(outputsDir);
        Files.createDirectories(tempDir);
    }

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadVideo(@RequestParam("video") MultipartFile videoFile) {
        Map<String, Object> response = new HashMap<>();

        String originalName = videoFile.getOriginalFilename();
        if (originalName == null || !isValidVideoFile(originalName)) {
            response.put("success", false);
            response.put("message", "Formato de arquivo inválido. Use: mp4, avi, mov, mkv");
            return ResponseEntity.badRequest().body(response);
        }

        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String filename = timestamp + "_" + originalName;
        Path videoPath = uploadsDir.resolve(filename);

        try {
            Files.copy(videoFile.getInputStream(), videoPath);
            Map<String, Object> result = processVideo(videoPath, timestamp);
            Files.deleteIfExists(videoPath);
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            response.put("success", false);
            response.put("message", "Erro ao salvar o vídeo: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/download/{filename}")
    public ResponseEntity<Resource> downloadZip(@PathVariable String filename) throws IOException {
        Path filePath = outputsDir.resolve(filename);
        if (!Files.exists(filePath)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }

        Resource file = new FileSystemResource(filePath);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        return new ResponseEntity<>(file, headers, HttpStatus.OK);
    }

    @GetMapping("/api/status")
    public ResponseEntity<Map<String, Object>> status() throws IOException {
        Map<String, Object> response = new HashMap<>();
        List<Map<String, Object>> filesList = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(outputsDir, "*.zip")) {
            for (Path file : stream) {
                Map<String, Object> fileInfo = new HashMap<>();
                fileInfo.put("filename", file.getFileName().toString());
                fileInfo.put("size", Files.size(file));
                fileInfo.put("created_at", Files.getLastModifiedTime(file).toString());
                fileInfo.put("download_url", "/download/" + file.getFileName());
                filesList.add(fileInfo);
            }
        }

        response.put("files", filesList);
        response.put("total", filesList.size());
        return ResponseEntity.ok(response);
    }

    private Map<String, Object> processVideo(Path videoPath, String timestamp) {
        Map<String, Object> result = new HashMap<>();
        Path workingDir = tempDir.resolve(timestamp);

        try {
            Files.createDirectories(workingDir);

            String framePattern = workingDir.resolve("frame_%04d.png").toString();

            ProcessBuilder pb = new ProcessBuilder(
                    "ffmpeg", "-i", videoPath.toString(), "-vf", "fps=1", "-y", framePattern
            );
            Process process = pb.redirectErrorStream(true).start();

            String output = new String(process.getInputStream().readAllBytes());
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                result.put("success", false);
                result.put("message", "Erro no ffmpeg: " + output);
                return result;
            }

            List<Path> frames = Files.list(workingDir)
                    .filter(p -> p.toString().endsWith(".png"))
                    .toList();

            if (frames.isEmpty()) {
                result.put("success", false);
                result.put("message", "Nenhum frame foi extraído do vídeo");
                return result;
            }

            Path zipPath = outputsDir.resolve("frames_" + timestamp + ".zip");
            createZipFile(frames, zipPath);

            List<String> imageNames = frames.stream()
                    .map(p -> p.getFileName().toString())
                    .toList();

            result.put("success", true);
            result.put("message", "Processamento concluído com sucesso!");
            result.put("zip_path", zipPath.getFileName().toString());
            result.put("frame_count", frames.size());
            result.put("images", imageNames);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "Erro durante o processamento: " + e.getMessage());
        } finally {
            FileSystemUtils.deleteRecursively(workingDir.toFile());
        }

        return result;
    }

    private void createZipFile(List<Path> files, Path zipPath) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipPath))) {
            for (Path file : files) {
                ZipEntry zipEntry = new ZipEntry(file.getFileName().toString());
                zos.putNextEntry(zipEntry);
                Files.copy(file, zos);
                zos.closeEntry();
            }
        }
    }

    private boolean isValidVideoFile(String filename) {
        String ext = filename.substring(filename.lastIndexOf(".")).toLowerCase();
        return List.of(".mp4", ".avi", ".mov", ".mkv", ".wmv", ".flv", ".webm").contains(ext);
    }
}
