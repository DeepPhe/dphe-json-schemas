package dphe.utils;


import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;


import java.nio.file.Path;


public class GitUtil {
    public static void cloneShallow(String repoUrl, String branch, Path toDir) throws GitAPIException {
        Git.cloneRepository()
                .setURI(repoUrl)
                .setDirectory(toDir.toFile())
                .setBranch(branch)
                .setDepth(1) // shallow
                .call()
                .close();
    }
}