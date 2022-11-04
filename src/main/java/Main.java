import beans.Commit;

import org.eclipse.jgit.api.errors.GitAPIException;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;


import java.util.List;

public class Main {
    public static void main(String[] args) {
        GitController gc = new GitController("openstack",
                "neutron",
                "https://github.com/openstack/neutron.git");
        List<Commit> commitList = gc.getAllCommits();

        /*
        try {
            gc.showTag();
        } catch (GitAPIException e) {
        }
        */

        /*
        for(Commit c: commitList){
            // System.out.println("--");
            // System.out.println(c.commitId);
            // System.out.println(c.parentCommitIds);
            // System.out.println(c.childCommitIds);
            // System.out.println(c.fixedReports);

            // System.out.println("--");
        }
        */

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream("/Users/mizuki-u/current/dataset/Neutron/csv/latest_rev_hash.csv")))) {
            String line;
            String line_hash;
            String hash;
            List<String> tag_hash = new ArrayList<String>();
            List<String> hash_list = new ArrayList<String>();
            List<String> hozon_list = new ArrayList<String>();
            List<String> result_list = new ArrayList<String>();
            // create tag_hash list start
            BufferedReader reader_taghash = new BufferedReader(new InputStreamReader(
                    new FileInputStream("/Users/mizuki-u/current/dataset/Neutron/csv/TagHash.csv")));
            reader.readLine();
            reader_taghash.readLine();
            while ((line_hash = reader_taghash.readLine()) != null) {
                String[] hash_data = line_hash.split(",");
                tag_hash.add(hash_data[1]);
            }
            // goal
            int count = 0;
            int count_process = 0;
            while ((line = reader.readLine()) != null) {
                count_process++;
                System.out.println(count_process);
                if(count_process == 2){
                    break;
                }
                // System.out.println("------------------------------");
                String[] data = line.split(",");
                int Merge = Integer.parseInt(data[4]);
                if(Merge == 0){
                    // System.out.println(data[1]);
                    continue;
                }
                count++;
                hash = data[3];
                // System.out.println(hash_list);
                List<String> parent = new ArrayList<String>();
                parent.add("991ea0b923c13f97d96557b752b6377143dcb5c4");
                        // List<String> parent = gc.getParent(commitList, hash);
                List<String> Tag = gc.getTag(tag_hash, commitList, parent, hozon_list);
                int num = Integer.parseInt(data[1]);
                if(!Tag.isEmpty()){
                    for(int i = 0; i < Tag.size(); i++){
                        // System.out.println(Tag.get(i));
                        result_list.add(Tag.get(i));
                    }
                    // result_list.add(Tag.toString());
                    System.out.println("Result");
                }
                // System.out.println("Number:" + num + ", Tag:" + Tag);
            }
            System.out.println(result_list);
            System.out.println(result_list.size());
            System.out.println(count);
            System.out.println(commitList.size());
        } catch (IOException e) {
            System.out.println("ファイル読み込みに失敗");
        }

        /*
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream("/Users/mizuki-u/current/dataset/Neutron/csv/latest_rev_hash.csv")))) {
            String line;
            String line_hash;
            String hash;
            List<String> tag_hash = new ArrayList<String>();
            List<String> hash_list = new ArrayList<String>();
            List<String> hozon_list = new ArrayList<String>();
            List<String> result_list = new ArrayList<String>();
            // create tag_hash list start
            BufferedReader reader_taghash = new BufferedReader(new InputStreamReader(
                    new FileInputStream("/Users/mizuki-u/current/dataset/Neutron/csv/TagHash.csv")));
            reader.readLine();
            reader_taghash.readLine();
            while ((line_hash = reader_taghash.readLine()) != null) {
                String[] hash_data = line_hash.split(",");
                tag_hash.add(hash_data[1]);
            }
            // goal
            int count = 0;
            while ((line = reader.readLine()) != null) {
                // System.out.println("------------------------------");
                String[] data = line.split(",");
                int Merge = Integer.parseInt(data[4]);
                if(Merge == 0){
                    // System.out.println(data[1]);
                    continue;
                }
                count++;
                hash = data[3];
                hash_list.add(hash);
                // System.out.println(hash_list);
                List<String> Tag = gc.getTag(tag_hash, commitList, hash_list, hozon_list);
                int num = Integer.parseInt(data[1]);
                if(!Tag.isEmpty()){
                    for(int i = 0; i < Tag.size(); i++){
                        // System.out.println(Tag.get(i));
                        result_list.add(Tag.get(i));
                    }
                    // result_list.add(Tag.toString());
                    // System.out.println("Result");
                }
                System.out.println("Number:" + num + ", Tag:" + Tag);
                hash_list.clear();

            }
            System.out.println(result_list);
            System.out.println(result_list.size());
            System.out.println(count);
            System.out.println(commitList.size());
        } catch (IOException e) {
            System.out.println("ファイル読み込みに失敗");
        }
        */

        /*
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream("/Users/mizuki-u/current/dataset/Neutron/csv/latest_rev_hash.csv")))) {
            String line;
            String hash;
            reader.readLine();
            int count = 0;
            int read = 0;
            int rev_all = 0;
            while ((line = reader.readLine()) != null) {
                read++;
                String[] data = line.split(",");
                int Merge = Integer.parseInt(data[4]);
                rev_all += Integer.parseInt(data[2]);
                if (Merge == 0) {
                    continue;
                }
                hash = data[3];

                for(Commit c: commitList) {
                    String commit_hash = c.commitId;
                    // System.out.println("Hash:" + hash);
                    // System.out.println("Tag:" + commit_hash);
                    if (hash.equals(commit_hash)) {
                        System.out.println("Hash:" + hash + ", Tag:" + commit_hash);
                        System.out.println(c.childCommitIds);
                        count++;
                        System.out.println("number:" + data[1]);
                        break;
                    }
                }


            }
            System.out.println(count);
            System.out.println(read);
            System.out.println(rev_all);
        } catch (IOException e) {
            System.out.println("ファイル読み込みに失敗");
        }
        */

    }
}
