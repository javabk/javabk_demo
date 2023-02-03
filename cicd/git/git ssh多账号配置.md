# git 多SSH key配置以及常见问题

### 针对每个账户生成不同的 SSH Key

```
ssh-keygen -t rsa -C "<邮箱,如：xxx@qq.com>"
Generating public/private rsa key pair.
Enter file in which to save the key (/c/Users/Administrator/.ssh/id_rsa): <输入生成文件的名称，随意命名>
后面一路直接回车，不需要输入密码
```

例如：

生成第一个 SSH Key：javabk_test_rsa

```shell
$ ssh-keygen -t rsa -C "javabk@qq.com"
Generating public/private rsa key pair.
Enter file in which to save the key (/c/Users/Administrator/.ssh/id_rsa): javabk_test_rsa
Enter passphrase (empty for no passphrase):
Enter same passphrase again:
Your identification has been saved in javabk_test_rsa
Your public key has been saved in javabk_test_rsa.pub
...//省略
```

生成第二个 SSH Key: javabk2_test_rsa

```shell
$ ssh-keygen -t rsa -C "javabk2@qq.com"
Generating public/private rsa key pair.
Enter file in which to save the key (/c/Users/Administrator/.ssh/id_rsa): javabk2_test_rsa
Enter passphrase (empty for no passphrase):
Enter same passphrase again:
Your identification has been saved in javabk2_test_rsa
Your public key has been saved in javabk2_test_rsa.pub
...//省略
```

### ssh-add 添加到高速缓存(可选)

通过 ssh-add 命令将专用私钥添加到[ssh-agent](https://www.jianshu.com/p/3e20853abc9b)的高速缓存中，提升性能

```
ssh-add ~/.ssh/javabk_test_rsa
ssh-add ~/.ssh/javabk2_test_rsa
```



### 添加 SSH 公钥到服务端

将上面每个生成的 .pub 里面的内容全部配置到仓库的SSH KEY，如上面的例子，将 javabk_test_rsa.pub 文件的内容输出，然后复制到服务端对应配置SSH KEY的地方。



### 配置 ~/.ssh/config 文件

如果 ~/.ssh/config 文件不存在，则创建一个。配置如下：

```
Host javabk_github.com
        HostName github.com
        PreferredAuthentications publickey
        IdentityFile ~/.ssh/javabk_test_rsa
        User javabk

Host javabk2_github.com
        HostName github.com
        PreferredAuthentications publickey
        IdentityFile ~/.ssh/javabk2_test_rsa
        User javabk2

```

参数说明: 

Host: 随意命名，保证唯一，建议用户名 + 仓库域名地址

HostName: 仓库域名地址

PreferredAuthentications: 强制使用Public Key验证

IdentityFile: 密钥文件的路径

User: 指定用户名



### 常见问题

1. git push 提交时报权限拒绝错误，如下：

   ```shell
   $ git push
   ERROR: Permission to xxxx.git denied to xxx用户名.
   fatal: Could not read from remote repository.
   
   Please make sure you have the correct access rights
   and the repository exists.
   ```

   解决思路：

   1. 检查报错的用户名是否是提交仓库应该用到的用户名，如果不是，检查 .ssh/config 里面对应的Host 有没有指定 User；同时如果是同个仓库多个用户名，检查提交代码工程的 .git/config 文件中，[remote "origin"] 下面的 url = git@xxx**Host**:yyy.git 中的 Host是否跟 .ssh/config 的Host配置对应，否则可以修改想要的Host，这样才会用到相关的配置进行提交。 

   错误案例：：

   + ~/.ssh/config , 同个网站，多个用户名。其中第一个Host的用户名是javabk，第二个用户名是 javabk2

     ```
     Host github.com
             HostName github.com
             PreferredAuthentications publickey
             IdentityFile ~/.ssh/javabk_test_rsa
     
     Host javabk2_github.com
             HostName github.com
             PreferredAuthentications publickey
             IdentityFile ~/.ssh/javabk2_test_rsa
     ```

   + git clone 代码后，工程里面的 .git/config 如下。侧重看：url = git@github.com 中的 **github.com**，这个其实是对应到上面的 ~/.ssh/config 里面 Host 为

     github.com 配置

     ```
     [core]
             repositoryformatversion = 0
             filemode = false
             bare = false
             logallrefupdates = true
             symlinks = false
             ignorecase = true
     [remote "origin"]
             url = git@github.com:javabk/javabk_demo.git
             fetch = +refs/heads/*:refs/remotes/origin/*
     [branch "main"]
             remote = origin
             merge = refs/heads/main
     
     ```

   + javabk2用户的仓库代码进行git push 提交时报错，如下，发现用户名并不是 javabk2，而是javabk，应该是没指定时使用了默认账号

     ```
     $ git push
     ERROR: Permission to xxxx.git denied to javabk.
     fatal: Could not read from remote repository.
     
     Please make sure you have the correct access rights
     and the repository exists.
     ```

     解决：

     1. 修改代码工程里面的 .git/config 中 [remote "origin"] 下的 url = git@github.com:javabk/javabk.demo.git 中的 **github.com** 改成 **javabk2_github.com**（对应 ~/.ssh/config 中对应第二个用户名的Host 配置）。如果这步还不行，尝试执行2

        ```
        url = git@javabk2_github.com:javabk/javabk_demo.git
        ```

     2. 在仓库代码里面指定对应的用户名和邮箱

        ```
        git config user.name "javabk2"
        git config user.email "javabk2@qq.com"
        ```

        或者直接编辑工程里面的 .git/config，添加下面这部分内容（其实上面的命令就是添加这部分内容）

        ```
        [user]
                name = javabk2
                email = javabk2@qq.com
        ```