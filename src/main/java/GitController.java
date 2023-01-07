

import beans.Commit;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.RevTag;
import org.refactoringminer.api.GitService;
import org.refactoringminer.util.GitServiceImpl;
import utils.MyFileUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.IOException;
import java.util.stream.Collectors;


import static java.time.temporal.ChronoUnit.DAYS;

public class GitController {
    /**
     * project name
     */
    public String project;
    /**
     * target id of the commit to be cloned
     */
    public String commitId;
    /**
     * repository directory path to be cloned
     */
    private final String repoDir;
    /**
     * GitHub's url (Other service is also acceptable)
     */
    private String url;
    /**
     * The Implement of JGit provided by Refactoring Miner
     */
    private final GitService git;
    /**
     * This is a class in JGit, which is used to provide
     * some functionalities that refactoring miner does not have
     */
    private Repository repo;


    public GitController(String projectName, String repoDir, String url) {
        this.repoDir = repoDir;
        this.url = url;
        this.git = new GitServiceImpl2();
        this.init(false);
        this.project = projectName;
    }

    private void init(boolean deleteDir) {
        try {
            if (deleteDir) {
                this.deleteRepo();
            }
            this.repo = git.cloneIfNotExists(this.repoDir, this.url);
        } catch (Exception e) {
            throw new AssertionError();
        }
    }

    public Repository getRepo() {
        return this.repo;
    }

    /**
     * hard reset to HEAD
     */
    protected void hardReset() {
        try {
            new Git(this.repo).reset().setMode(ResetCommand.ResetType.HARD).call();
        } catch (GitAPIException e) {
            throw new AssertionError();
        }
    }

    /**
     * hard reset to a revision
     */
    protected void hardReset(String hash) {
        try {
            new Git(this.repo).reset().setRef(hash).setMode(ResetCommand.ResetType.HARD).call();
        } catch (GitAPIException e) {
            throw new AssertionError();
        }
    }

    /**
     * check out a revision
     * when parent is true, the parent revision will be checkout.
     * If the id is the first revision, NoParentsException will be thrown.
     * When ignoreNoParents is true, this exception won't be thrown.
     *
     * @param id
     * @param parent
     * @param ignoreNoParents
     */
    public void checkout(String id, boolean parent, boolean ignoreNoParents) throws NoParentsException {
        RevCommit commit = this.findObjectId(id, ignoreNoParents);
        if (parent) {
            commit = this.findObjectId(commit.getParent(0).getName());
        }
        this.commitId = commit.getName();
        try {
            this.hardReset(commit.getId().getName());
            git.checkout(this.repo, commit.getId().getName());
        } catch (CheckoutConflictException ce) {
            System.out.println(ce.getMessage());
            this.deleteRepo();
            try {
                this.repo = git.cloneIfNotExists(this.repoDir, this.url);
                this.hardReset(commit.getId().getName());
                git.checkout(this.repo, commit.getId().getName());
                System.err.println("CheckoutConflictException");
            } catch (Exception e) {
                System.err.println("Cannot clone");
                throw new AssertionError();
            }
        } catch (Exception e) {

            throw new AssertionError();
        }
    }

    public void checkout(String id, boolean ignoreNoParents) throws NoParentsException {
        this.checkout(id, false, ignoreNoParents);
    }

    public void checkout(String id) throws NoParentsException {
        this.checkout(id, false, false);
    }

    /**
     * get RevCommit in JGit based on id in String
     *
     * @param commitId
     * @return
     * @throws NoParentsException
     */
    protected RevCommit findObjectId(String commitId) throws NoParentsException {
        return findObjectId(commitId, false);
    }

    protected RevCommit findObjectId(String commitId, boolean ignoreNoParents) throws NoParentsException {
        try {
            ObjectId evalCommitId = this.repo.resolve(commitId);
            try (RevWalk walk = new RevWalk(this.repo)) {
                RevCommit evalCommit = walk.parseCommit(evalCommitId);
                if (evalCommit.getParentCount() == 0) {
                    if (ignoreNoParents) {
                        walk.dispose();
                        return evalCommit;
                    }
                    throw new NoParentsException();
                }
                walk.parseCommit(evalCommit.getParent(0).getId());
                walk.dispose();
                return evalCommit;
            }
        } catch (IOException ex) {
            throw new AssertionError();
        }
    }

    public String getRepoDir() {
        return this.repoDir;
    }

    /**
     * create a Commit from RevCommit
     *
     * @param ignoreNoParents
     * @return
     * @throws NoParentsException
     */
    public Commit getCommit(boolean ignoreNoParents) throws NoParentsException {
        RevCommit commit = this.findObjectId(commitId, ignoreNoParents);
        return ((GitServiceImpl2) this.git).getCommit(project, repo, commit);
    }

    public Commit getCommit() throws NoParentsException {
        return getCommit(false);
    }

    public void deleteRepo() {
        try {
            MyFileUtils.deleteDirectory(this.repoDir);
        } catch (IOException e) {
        }
    }

    /**
     * get parent's commit id
     *
     * @return
     * @throws NoParentsException
     */
    public String getParentCommitId() throws NoParentsException {
        RevCommit commit = this.findObjectId(commitId);
        return commit.getParent(0).getName();
    }

    /**
     * get a list of all the commits from all branches
     *
     * @return
     */
    public List<Commit> getAllCommits(String branch) {
        List<Commit> commits = null;
        try {
            commits = ((GitServiceImpl2) git).getAllCommits(this.project, repo, branch);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException();
        }
        setChildren(commits);
        return commits;
    }

    public List<Commit> getAllCommits() {
        return getAllCommits(null);
    }

    /**
     * receive a list of hash ids and returns a list of commits
     * NOTE: This does not store childrens
     *
     * @param targetsId
     * @return
     * @throws NoParentsException
     */
    public List<Commit> getCommits(List<String> targetsId) throws NoParentsException {
        List<Commit> commits = new ArrayList<>();
        for (String id : targetsId) {
            commits.add(getCommit(id));
        }
        return commits;
    }

    /**
     * receive a hash id and returns a commit
     *
     * @param targetsId
     * @return
     * @throws NoParentsException
     */
    public Commit getCommit(String targetsId) throws NoParentsException {
        RevCommit commit = this.findObjectId(targetsId, true);
        return ((GitServiceImpl2) this.git).getCommit(project, repo, commit);
    }

    public void setChildren(List<Commit> commits) {
        Map<String, Commit> map = transform2Map(commits);
        for (int i = 0; i < commits.size(); i++) {
            Commit c = commits.get(i);
            for (String parent : c.parentCommitIds) {
                Commit p = map.get(parent);
                p.childCommitIds.add(c.commitId);
            }
        }
    }

    public static Map<String, Commit> transform2Map(List<Commit> commits) {
        Map<String, Commit> map = new HashMap<String, Commit>();
        for (Commit c : commits) {
            map.put(c.commitId, c);
        }
        return map;
    }

    public Integer countCommitsBetween(String parent, String child) {
        try {
            List<RevCommit> commits = (List<RevCommit>) this.git.createRevsWalkBetweenCommits(this.repo, parent, child);
            return commits.size();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Long getDays(Commit parent, Commit child) {
        if (parent == null | child == null) {
            return null;
        }
        return DAYS.between(parent.commitDate, child.commitDate);
    }

    public void showTag() throws GitAPIException {
        List<String> tag_number = new ArrayList<String>();
        List<String> commit_hash = new ArrayList<String>();
        try (RevWalk walk = new RevWalk(this.repo)) {
            List<Ref> list = new Git(this.repo).tagList().call();
            for (Ref ref : list) {
                System.out.println(ref.toString());
                String cut = ref.toString().substring(14);
                int index1 = cut.indexOf("=");
                String result_tag = cut.substring(0, index1);
                tag_number.add(result_tag);
                System.out.println(result_tag);
                String cuts = cut.substring(index1 + 1);
                int index2 = cuts.indexOf("(");
                String result_hash = cuts.substring(0, index2);
                commit_hash.add(result_hash);
            }
            System.out.println(commit_hash);
        }
        exportCsv(tag_number, commit_hash);
    }

    public static void exportCsv(List<String> tag, List<String> hash) {
        try {
            // 出力ファイルの作成
            FileWriter fw = new FileWriter("/Users/mizuki-u/current/TagHash.csv", false);
            // PrintWriterクラスのオブジェクトを生成
            PrintWriter pw = new PrintWriter(new BufferedWriter(fw));

            // ヘッダーの指定
            pw.print("tag");
            pw.print(",");
            pw.print("hash");
            pw.println();

            // データを書き込む
            for (int i = 0; i < tag.size(); i++) {
                pw.print(tag.get(i));
                pw.print(",");
                pw.print(hash.get(i));
                pw.println();
            }

            // ファイルを閉じる
            pw.close();

            // 出力確認用のメッセージ
            System.out.println("csvファイルを出力しました");

            // 出力に失敗したときの処理
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static List<String> getTag(List<String> tag_hash, List<Commit> commitList, List<String> hash_list, List<String> hozon_list){
        // hash_listの子要素のhash値list
        List<String> child_list = new ArrayList<String>();
        // tagのhash値list
        List<String> tag_list = new ArrayList<String>();
        // child_listを全て格納して再帰する際の親hash値listとして使う
        List<String> new_hash_list = new ArrayList<String>();
        // returnする重複を除いたtagのhash値list
        List<String> tag_list_unique = new ArrayList<String>();


        int count = 0;
        // hash_listから1つずつ探索開始
        // System.out.println("hashList: " + hash_list);
        loop: for(String h: hash_list){
            // 何個目のhashかをcount
            count++;
            for (String Tag : tag_hash) {
                // 同じであれば
                if (h.equals(Tag)) {
                    // タグのハッシュ値を出力してtag_listにtagをaddする
                    // System.out.println("Tag: " + Tag);
                    tag_list.add(Tag);
                    break;
                }
            }
            // 1つ1つのコミットについて
            for(Commit c: commitList){
                // コミットハッシュと親ハッシュが同じ場合
                if(h.equals(c.commitId)) {
                    // その子ハッシュlistをchild_listに格納
                    child_list = c.childCommitIds;
                    // System.out.println(child_list);
                    // 子ハッシュlistから1つずつ取り出し
                    for (String child : child_list) {
                        // タグのハッシュ値を1つずつ取り出し
                        for (String Tag : tag_hash) {
                            // 同じであれば
                            if (child.equals(Tag)) {
                                // タグのハッシュ値を出力してtag_listにtagをaddする
                                // System.out.println("Tag: " + Tag);
                                tag_list.add(Tag);
                                break;
                            }
                        }
                    }
                    // タグがあれば
                    if (!tag_list.isEmpty()) {
                        // 親のハッシュ値を全て見ていれば
                        if (count == hash_list.size()) {
                            // System.out.println("ifif");
                            tag_list_unique = tag_list.stream().distinct().collect(Collectors.toList());
                            return tag_list_unique;
                        } else {
                            // System.out.println("ifelse");
                            break;
                        }
                    }
                    // タグがなければ
                    else {
                        // 親のハッシュ値を全て見ていれば
                        if (count == hash_list.size()) {
                            // System.out.println("elseif");
                            // 何度も同じハッシュ値を確認しないために前々回までに被っていれば追加しない
                            for(int i = 0; i < child_list.size(); i++) {
                                if(!new_hash_list.contains(child_list.get(i))){
                                    if(!hozon_list.contains(child_list.get(i))){
                                        if(!hash_list.contains(child_list.get(i))){
                                            new_hash_list.add(child_list.get(i));
                                        }
                                    }
                                }
                            }
                            // new_hash_listにhashが無ければNoHashを格納してreturn
                            if(new_hash_list.isEmpty()){
                                break loop;
                            }
                            hozon_list = hash_list;
                            return getTag(tag_hash, commitList, new_hash_list, hozon_list);
                        } else {
                            // System.out.println("elseelse");
                            // 何度も同じハッシュ値を確認しないために前々回までに被っていれば追加しない
                            for(int i = 0; i < child_list.size(); i++) {
                                if(!new_hash_list.contains(child_list.get(i))){
                                    if(!hozon_list.contains(child_list.get(i))){
                                        if(!hash_list.contains(child_list.get(i))){
                                            new_hash_list.add(child_list.get(i));
                                        }
                                    }
                                }
                            }
                            break;
                        }
                    }
                }
            }
        }
        return tag_list;
    }

    public static List<String> getParent(List<Commit> commitList, String hash){
        List<String> parent_list = new ArrayList<String>();
        // 1つ1つのコミットについて
        for(Commit c: commitList){
            // コミットハッシュとハッシュが同じ場合
            if(hash.equals(c.commitId)) {
                // その親ハッシュlistをparent_listに格納
                parent_list = c.parentCommitIds;
            }
            else {
                parent_list.add(hash);
            }
            break;
        }
        return parent_list;
    }

}
