# YWS1 Editor for Android

「妖怪ウォッチ1 スマホ」の`main.bin`をrootモードのShizuku経由で読み込み、
`game0.yw`〜`game3.yw`のデータを編集して保存するAndroidアプリです。

## 実装内容

- `crypt.py` / `check.py` の暗号・復号ロジックを Kotlin に移植
- `main.bin` から `game0.yw` / `game1.yw` / `game2.yw` / `game3.yw` を選択して妖怪リストを抽出
- 個体値などを Compose UI で編集
- どうぐの一覧表示と、安全な範囲での既存数量変更
- そうび、だいじなもの、ガシャstate、さすらい荘の読取り専用表示
- 保存時に`main.bin.bak`を作成し、一時ファイルを検証してから置換

## 対象ファイル

- `/data/user/<現在のユーザーID>/jp.co.level5.yws1/files/save/main.bin`

## 使い方

1. rootモードでShizukuを起動
2. アプリを起動し、表示されたShizuku許可ダイアログを許可
3. `game0`〜`game3`から編集対象を選択
4. 妖怪やセーブ情報を編集
5. `保存`で選択中セーブへ反映

## 注意

- ADBモード（非root）のShizukuでは、他アプリの内部データへアクセスできないため編集できません。
- 書き込み前に自動バックアップ（`.bak`）を作成しますが、自己責任で利用してください。

## ライセンス

- このプロジェクトのコードは MIT License で公開しています。詳細は `LICENSE` を参照してください。
- 同梱している第三者著作物のライセンス情報は `THIRD_PARTY_NOTICES.md` を参照してください。
