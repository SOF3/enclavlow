package io.github.sof3.enclavlow.example;

import edu.hku.cs.uranus.IntelSGX;
import edu.hku.cs.uranus.IntelSGXOcall;
import io.github.sof3.enclavlow.api.Enclavlow;
import org.jetbrains.annotations.Nullable;
import spark.Spark;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.security.DigestOutputStream;
import java.security.Key;
import java.security.MessageDigest;

public class Entry {
    public static void main(String[] args) {
        Spark.get("/sha512/:file", (req, res) -> {
            String file = req.params("file");
            File path = new File(".", file);
            return hashFile(path);
        });
    }

    static byte[] getSecret() {
        return Enclavlow.sourceMarker(new byte[]{1, 2, 3, 4, 5, 6, 7, 8});
    }

    @IntelSGXOcall
    static byte[] readFile(File file) throws IOException {
        var baos = new ByteArrayOutputStream();
        try(var fis = new FileInputStream(file)){
            fis.transferTo(baos);
        }
        return baos.toByteArray();
    }

    @IntelSGX
    @Nullable
    static byte[] hashFile(File file) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC");
            Key key = new SecretKeySpec(getSecret(), "AES");
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] contents = readFile(file);

            var cis = new CipherInputStream(new ByteArrayInputStream(contents), cipher);

            var baos = new ByteArrayOutputStream();
            MessageDigest digest = MessageDigest.getInstance("SHA-512");
            var dos = new DigestOutputStream(baos, digest);

            int read;
            for(byte[] buffer = new byte[8192]; (read = cis.read(buffer, 0, 8192)) >= 0; ) {
                dos.write(buffer, 0, read);
            }

            return baos.toByteArray();
        } catch (Throwable e){
            return null;
        }
    }
}
