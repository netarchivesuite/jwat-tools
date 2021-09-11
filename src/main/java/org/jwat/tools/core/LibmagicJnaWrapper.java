package org.jwat.tools.core;

import java.io.IOException;
import java.nio.Buffer;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Platform;
import com.sun.jna.Pointer;

/**
 * A wrapper for the libmagic library that relies on JNA.
 *
 * @author hbian
 */
public class LibmagicJnaWrapper {

	/**
	 * LibmagicDll JNA interface.
	 */
	public interface LibmagicDll extends Library {
        String LIBRARY_NAME = (Platform.isWindows())? "magic1": "magic";
        LibmagicDll BASE = (LibmagicDll)Native.loadLibrary(LIBRARY_NAME, LibmagicDll.class);
        LibmagicDll INSTANCE = (LibmagicDll)Native.synchronizedLibrary(BASE);

        Pointer magic_open(int flags);
        void magic_close(Pointer cookie);
        int magic_setflags(Pointer cookie, int flags);

        String magic_file(Pointer cookie, String fileName);
        String magic_buffer(Pointer cookie, Buffer buffer, NativeLong length);

        int magic_compile(Pointer cookie, String magicFileName);
        int magic_check(Pointer cookie, String magicFileName);
        int magic_load(Pointer cookie, String magicFileName);

        int magic_errno(Pointer cookie);
        String magic_error(Pointer cookie);
    }

    /** Libmagic flag: No flags. */
    public final static int MAGIC_NONE                  = 0x000000;
    /** Libmagic flag: Turn on debugging. */
    public final static int MAGIC_DEBUG                 = 0x000001;
    /** Libmagic flag: Follow symlinks. */
    public final static int MAGIC_SYMLINK               = 0x000002;
    /** Libmagic flag: Check inside compressed files. */
    public final static int MAGIC_COMPRESS              = 0x000004;
    /** Libmagic flag: Look at the contents of devices. */
    public final static int MAGIC_DEVICES               = 0x000008;
    /** Libmagic flag: Return the MIME type. */
    public final static int MAGIC_MIME_TYPE             = 0x000010;
    /** Libmagic flag: Return all matches. */
    public final static int MAGIC_CONTINUE              = 0x000020;
    /** Libmagic flag: Print warnings to stderr. */
    public final static int MAGIC_CHECK                 = 0x000040;
    /** Libmagic flag: Restore access time on exit. */
    public final static int MAGIC_PRESERVE_ATIME        = 0x000080;
    /** Libmagic flag: Don't translate unprintable chars. */
    public final static int MAGIC_RAW                   = 0x000100;
    /** Libmagic flag: Handle ENOENT etc as real errors. */
    public final static int MAGIC_ERROR                 = 0x000200;
    /** Libmagic flag: Return the MIME encoding. */
    public final static int MAGIC_MIME_ENCODING         = 0x000400;
    /** Libmagic flag: Return both MIME type and encoding. */
    public final static int MAGIC_MIME                  = (MAGIC_MIME_TYPE | MAGIC_MIME_ENCODING);
    /** Libmagic flag: Return the Apple creator and type. */
    public final static int MAGIC_APPLE                 = 0x000800;
    /** Libmagic flag: Don't check for compressed files. */
    public final static int MAGIC_NO_CHECK_COMPRESS     = 0x001000;
    /** Libmagic flag: Don't check for tar files. */
    public final static int MAGIC_NO_CHECK_TAR          = 0x002000;
    /** Libmagic flag: Don't check magic entries. */
    public final static int MAGIC_NO_CHECK_SOFT         = 0x004000;
    /** Libmagic flag: Don't check application type. */
    public final static int MAGIC_NO_CHECK_APPTYPE      = 0x008000;
    /** Libmagic flag: Don't check for elf details. */
    public final static int MAGIC_NO_CHECK_ELF          = 0x010000;
    /** Libmagic flag: Don't check for text files. */
    public final static int MAGIC_NO_CHECK_TEXT         = 0x020000;
    /** Libmagic flag: Don't check for cdf files. */
    public final static int MAGIC_NO_CHECK_CDF          = 0x040000;
    /** Libmagic flag: Don't check tokens. */
    public final static int MAGIC_NO_CHECK_TOKENS       = 0x100000;
    /** Libmagic flag: Don't check text encodings. */
    public final static int MAGIC_NO_CHECK_ENCODING     = 0x200000;

    /** Magic cookie pointer. */
    private final Pointer cookie;

    /**
     * Creates a new instance returning the default information: MIME
     * type and character encoding.
     *
     * @throws IOException   if any error occurred while
     *         initializing the libmagic.
     *
     * @see    #LibmagicJnaWrapper(int)
     * @see    #MAGIC_MIME
     */
    public LibmagicJnaWrapper() throws IOException {
        this(MAGIC_MIME | MAGIC_SYMLINK);
    }

    /**
     * Creates a new instance returning the information specified in
     * the <code>flag</code> argument
     * @param flag <code>Libmagic</code> flags
     *
     * @throws IOException   if any error occurred while
     *         initializing the libmagic.
     */
    public LibmagicJnaWrapper(int flag) throws IOException {
        cookie = LibmagicDll.INSTANCE.magic_open(flag);
        if (cookie == null) {
            throw new IOException("Libmagic initialization failed");
        }
    }

    /**
     * Closes the magic database and deallocates any resources used.
     */
    public void close() {
        LibmagicDll.INSTANCE.magic_close(cookie);
    }

    /**
     * Returns a textual explanation of the last error.
     * @return the textual description of the last error, or
     *         <code>null</code> if there was no error.
     */
    public String getError() {
        return LibmagicDll.INSTANCE.magic_error(cookie);
    }

    /**
     * Returns the textual description of the contents of the
     * specified file.
     * @param filePath   the path of the file to be identified.
     *
     * @return the textual description of the file, or
     *         <code>null</code> if an error occurred.
     */
    public String getMimeType(String filePath) {
        if ((filePath == null) || (filePath.length() == 0)) {
            throw new IllegalArgumentException("filePath");
        }
        return LibmagicDll.INSTANCE.magic_file(cookie, filePath);
    }

    /**
     * Returns textual description of the contents of the
     * <code>buffer</code> argument.
     * @param buffer   the data to analyze.
     * @param length   the length, in bytes, of the buffer.
     *
     * @return the textual description of the buffer data, or
     *         <code>null</code> if an error occurred.
     */
    public String getMimeType(Buffer buffer, long length) {
        return LibmagicDll.INSTANCE.magic_buffer(cookie, buffer, new NativeLong(length));
    }

    /**
     * Compiles the colon-separated list of database text files passed
     * in as <code>magicFiles</code>.
     * @param magicFiles   the magic database file(s), or
     *                     <code>null</code> to use the default
     *                     database.
     * @return 0 on success and -1 on failure.
     */
    public int compile(String magicFiles) {
        return LibmagicDll.INSTANCE.magic_compile(cookie, magicFiles);
    }

    /**
     * Loads the colon-separated list of database files passed in as
     * <code>magicFiles</code>. This method must be used before any
     * magic queries be performed.
     * @param magicFiles   the magic database file(s), or
     *                     <code>null</code> to use the default
     *                     database.
     * @return 0 on success and -1 on failure.
     */
    public int load(String magicFiles) {
        return LibmagicDll.INSTANCE.magic_load(cookie, magicFiles);
    }

}
