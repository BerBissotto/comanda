package com.github.berbissotto.comanda.service;

import java.util.Optional;
import java.util.Set;

/**
 * Valida imagens enviadas pelo conteúdo real (bytes iniciais), nunca pelo
 * Content-Type informado pelo navegador. Só PNG, JPEG, WebP e GIF;
 * SVG e outros formatos que podem carregar script são recusados.
 */
public final class ImagemUpload {

    public static final Set<String> TIPOS_PERMITIDOS =
            Set.of("image/png", "image/jpeg", "image/webp", "image/gif");

    private ImagemUpload() {
    }

    /** Tipo detectado pelo conteúdo, ou vazio se não for uma imagem permitida. */
    public static Optional<String> detectarTipo(byte[] b) {
        if (b == null || b.length < 12) {
            return Optional.empty();
        }
        if ((b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G'
                && b[4] == 0x0D && b[5] == 0x0A && b[6] == 0x1A && b[7] == 0x0A) {
            return Optional.of("image/png");
        }
        if ((b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return Optional.of("image/jpeg");
        }
        if (b[0] == 'G' && b[1] == 'I' && b[2] == 'F' && b[3] == '8'
                && (b[4] == '7' || b[4] == '9') && b[5] == 'a') {
            return Optional.of("image/gif");
        }
        if (b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
            return Optional.of("image/webp");
        }
        return Optional.empty();
    }
}
