import beans.Commit;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        GitController gc = new GitController("neutron",
                "neutron",
                "https://github.com/openstack/neutron.git");
        List<Commit> commitList = gc.getAllCommits();
        try {
            gc.showTag();
        } catch (GitAPIException e) {
        }
        for(Commit c: commitList){
            System.out.println("--");
            System.out.println(c.commitId);
            System.out.println(c.parentCommitIds);
            System.out.println(c.childCommitIds);
            System.out.println(c.fixedReports);

            System.out.println("--");
        }
    }
}
