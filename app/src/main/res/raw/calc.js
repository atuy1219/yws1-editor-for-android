/*
  (calc.js)
  This file is part of Status Calculator for Yokai Watch

  Status Calculator for Yokai Watch
  Copyright 2015 IVC ◆HePDgfYKMA
  Released under the conditions of the MIT License
  http://opensource.org/licenses/mit-license.php
*/

yokaiData = [
    {"num": 1, "name": "ぶようじん坊", "furigana": "ぶようじんぼう", "bs": [29, 27, 19, 17, 26], "growPat": [2, 2, 3, 0, 0], "class": 1},
    {"num": 2, "name": "がらあきん坊", "furigana": "がらあきんぼう", "bs": [36, 33, 21, 18, 30], "growPat": [2, 2, 3, 0, 0], "class": 1},
    {"num": 3, "name": "さきがけの助", "furigana": "さきがけのすけ", "bs": [43, 36, 24, 18, 30], "growPat": [1, 1, 0, 0, 0], "class": 1},
    {"num": 4, "name": "ダラケ刀", "furigana": "だらけとう", "bs": [27, 26, 20, 23, 21], "growPat": [2, 2, 0, 0, 0], "class": 1},
    {"num": 5, "name": "ザンバラ刀", "furigana": "ざんばらとう", "bs": [34, 28, 22, 28, 26], "growPat": [2, 2, 0, 0, 0], "class": 1},
    {"num": 6, "name": "ダララだんびら", "furigana": "だららだんびら", "bs": [38, 31, 24, 29, 30], "growPat": [1, 1, 2, 2, 2], "class": 1},
    {"num": 7, "name": "ちからモチ", "furigana": "ちからもち", "bs": [34, 30, 17, 25, 21], "growPat": [0, 2, 3, 0, 3], "class": 1},
    {"num": 8, "name": "やきモチ", "furigana": "やきもち", "bs": [37, 32, 13, 28, 17], "growPat": [0, 2, 3, 0, 3], "class": 1},
    {"num": 9, "name": "カブトさん", "furigana": "かぶとさん", "bs": [33, 29, 17, 30, 19], "growPat": [0, 2, 3, 0, 0], "class": 1},
    {"num": 10, "name": "ゲンマ将軍", "furigana": "げんましょうぐん", "bs": [36, 35, 23, 30, 33], "growPat": [2, 2, 3, 2, 0], "class": 1},
    {"num": 11, "name": "黄泉ゲンスイ", "furigana": "よみげんすい", "bs": [36, 38, 27, 32, 38], "growPat": [2, 1, 0, 2, 0], "class": 1},
    {"num": 12, "name": "メラメライオン", "furigana": "めらめらいおん", "bs": [30, 28, 27, 20, 29], "growPat": [0, 0, 0, 0, 0], "class": 1},
    {"num": 13, "name": "グラグライオン", "furigana": "ぐらぐらいおん", "bs": [30, 32, 28, 23, 29], "growPat": [0, 0, 0, 0, 0], "class": 1},
    {"num": 14, "name": "轟獅子", "furigana": "とどろきじし", "bs": [31, 35, 31, 29, 38], "growPat": [2, 2, 2, 2, 2], "class": 1},
    {"num": 15, "name": "しょうブシ", "furigana": "しょうぶし", "bs": [31, 29, 24, 21, 27], "growPat": [2, 2, 0, 3, 0], "class": 1},
    {"num": 16, "name": "まさむね", "furigana": "まさむね", "bs": [37, 38, 23, 22, 37], "growPat": [2, 2, 0, 3, 0], "class": 1},
    {"num": 17, "name": "むらまさ", "furigana": "むらまさ", "bs": [33, 36, 29, 23, 36], "growPat": [2, 2, 0, 3, 0], "class": 1},
    {"num": 18, "name": "くさなぎ", "furigana": "くさなぎ", "bs": [33, 44, 25, 27, 42], "growPat": [2, 1, 0, 3, 2], "class": 1},
    {"num": 19, "name": "ベンケイ", "furigana": "べんけい", "bs": [35, 39, 18, 26, 30], "growPat": [2, 2, 3, 0, 0], "class": 1},
    {"num": 20, "name": "からくりベンケイ", "furigana": "からくりべんけい", "bs": [33, 36, 22, 23, 34], "growPat": [2, 2, 3, 0, 0], "class": 1},
    {"num": 21, "name": "やまと", "furigana": "やまと", "bs": [34, 33, 22, 25, 30], "growPat": [2, 2, 3, 3, 2], "class": 1},
    {"num": 22, "name": "カブキ猿", "furigana": "かぶきざる", "bs": [34, 33, 23, 25, 37], "growPat": [2, 2, 2, 3, 0], "class": 1},
    {"num": 23, "name": "クワノ武士", "furigana": "くわのぶし", "bs": [33, 30, 23, 29, 32], "growPat": [2, 0, 3, 2, 0], "class": 1},
    {"num": 24, "name": "クワガ大将", "furigana": "くわがたいしょう", "bs": [39, 39, 24, 28, 32], "growPat": [2, 0, 3, 2, 0], "class": 1},
    {"num": 25, "name": "オオクワノ神", "furigana": "おおくわのかみ", "bs": [43, 40, 25, 31, 35], "growPat": [2, 0, 3, 2, 0], "class": 1},
    {"num": 26, "name": "くしゃ武者", "furigana": "くしゃむしゃ", "bs": [36, 38, 21, 31, 40], "growPat": [0, 2, 3, 0, 0], "class": 1},
    {"num": 27, "name": "なまはげ", "furigana": "なまはげ", "bs": [37, 41, 24, 33, 39], "growPat": [2, 1, 0, 0, 0], "class": 1},
    {"num": 28, "name": "ズルズルづる", "furigana": "ずるずるづる", "bs": [24, 18, 26, 24, 27], "growPat": [0, 3, 2, 0, 2], "class": 2},
    {"num": 29, "name": "かぜカモ", "furigana": "かぜかも", "bs": [25, 19, 27, 24, 27], "growPat": [0, 3, 2, 0, 2], "class": 2},
    {"num": 30, "name": "わすれん帽", "furigana": "わすれんぼう", "bs": [23, 15, 28, 16, 37], "growPat": [2, 2, 2, 2, 2], "class": 2},
    {"num": 31, "name": "ばか頭巾", "furigana": "ばかずきん", "bs": [22, 19, 21, 20, 35], "growPat": [2, 2, 2, 2, 2], "class": 2},
    {"num": 32, "name": "つづかな僧", "furigana": "つづかなそう", "bs": [26, 18, 27, 20, 25], "growPat": [0, 2, 2, 3, 0], "class": 2},
    {"num": 33, "name": "やめたい師", "furigana": "やめたいし", "bs": [32, 22, 34, 20, 29], "growPat": [0, 2, 2, 3, 0], "class": 2},
    {"num": 34, "name": "もうせん和尚", "furigana": "もうせんおしょう", "bs": [34, 23, 36, 22, 29], "growPat": [2, 1, 1, 0, 2], "class": 2},
    {"num": 35, "name": "わらえ姉", "furigana": "わらえねえ", "bs": [26, 20, 29, 24, 28], "growPat": [0, 3, 2, 0, 0], "class": 2},
    {"num": 36, "name": "青くちびる", "furigana": "あおくちびる", "bs": [31, 22, 31, 25, 30], "growPat": [0, 3, 2, 0, 0], "class": 2},
    {"num": 37, "name": "バクロ婆", "furigana": "ばくろばあ", "bs": [27, 21, 31, 22, 28], "growPat": [0, 3, 2, 0, 2], "class": 2},
    {"num": 38, "name": "ババァーン", "furigana": "ばばぁーん", "bs": [30, 22, 39, 24, 34], "growPat": [0, 3, 2, 0, 2], "class": 2},
    {"num": 39, "name": "ドクロ婆", "furigana": "どくろばあ", "bs": [30, 22, 32, 25, 33], "growPat": [2, 0, 1, 2, 1], "class": 2},
    {"num": 40, "name": "モテモ天", "furigana": "もてもてん", "bs": [26, 20, 30, 22, 31], "growPat": [0, 0, 2, 0, 2], "class": 2},
    {"num": 41, "name": "モテマクール", "furigana": "もてまくーる", "bs": [33, 25, 39, 26, 35], "growPat": [0, 0, 2, 0, 2], "class": 2},
    {"num": 42, "name": "モテヌス", "furigana": "もてぬす", "bs": [31, 23, 43, 24, 37], "growPat": [0, 0, 2, 0, 2], "class": 2},
    {"num": 43, "name": "でんぱく小僧", "furigana": "でんぱくこぞう", "bs": [24, 24, 28, 22, 26], "growPat": [0, 0, 2, 0, 0], "class": 2},
    {"num": 44, "name": "でんじん", "furigana": "でんじん", "bs": [30, 29, 36, 29, 28], "growPat": [0, 0, 2, 0, 0], "class": 2},
    {"num": 45, "name": "はつでんしん", "furigana": "はつでんしん", "bs": [33, 34, 41, 32, 34], "growPat": [2, 2, 1, 0, 2], "class": 2},
    {"num": 46, "name": "うんがい鏡", "furigana": "うんがいきょう", "bs": [30, 20, 34, 30, 25], "growPat": [2, 3, 1, 2, 2], "class": 2},
    {"num": 47, "name": "ヤミ鏡", "furigana": "やみかがみ", "bs": [31, 23, 38, 38, 24], "growPat": [0, 0, 0, 2, 0], "class": 2},
    {"num": 48, "name": "まぼ老師", "furigana": "まぼろうし", "bs": [27, 19, 36, 23, 29], "growPat": [0, 3, 2, 0, 0], "class": 2},
    {"num": 49, "name": "かげ老師", "furigana": "かげろうし", "bs": [24, 17, 41, 20, 32], "growPat": [0, 3, 2, 0, 0], "class": 2},
    {"num": 50, "name": "ほう老師", "furigana": "ほうろうし", "bs": [29, 21, 38, 26, 34], "growPat": [0, 3, 2, 0, 0], "class": 2},
    {"num": 51, "name": "さとりちゃん", "furigana": "さとりちゃん", "bs": [31, 25, 32, 24, 32], "growPat": [2, 0, 2, 3, 2], "class": 2},
    {"num": 52, "name": "よつめ", "furigana": "よつめ", "bs": [30, 22, 36, 21, 33], "growPat": [2, 3, 2, 0, 2], "class": 2},
    {"num": 53, "name": "天狗", "furigana": "てんぐ", "bs": [29, 27, 27, 26, 22], "growPat": [2, 2, 3, 0, 1], "class": 2},
    {"num": 54, "name": "ほむら天狗", "furigana": "ほむらてんぐ", "bs": [34, 30, 33, 26, 19], "growPat": [2, 2, 3, 0, 1], "class": 2},
    {"num": 55, "name": "キュウビ", "furigana": "きゅうび", "bs": [32, 24, 44, 31, 38], "growPat": [2, 0, 2, 0, 0], "class": 2},
    {"num": 56, "name": "犬神", "furigana": "いぬがみ", "bs": [34, 26, 47, 30, 37], "growPat": [2, 0, 2, 0, 0], "class": 2},
    {"num": 57, "name": "だるだるま", "furigana": "だるだるま", "bs": [30, 26, 18, 26, 17], "growPat": [2, 0, 3, 2, 0], "class": 3},
    {"num": 58, "name": "だるまっちょ", "furigana": "だるまっちょ", "bs": [41, 30, 20, 35, 23], "growPat": [2, 0, 3, 2, 0], "class": 3},
    {"num": 59, "name": "ゴリだるま", "furigana": "ごりだるま", "bs": [41, 37, 19, 33, 19], "growPat": [2, 0, 3, 2, 0], "class": 3},
    {"num": 60, "name": "ムリカベ", "furigana": "むりかべ", "bs": [27, 24, 18, 28, 20], "growPat": [2, 0, 3, 2, 0], "class": 3},
    {"num": 61, "name": "むりだ城", "furigana": "むりだじょう", "bs": [38, 29, 21, 44, 27], "growPat": [2, 0, 3, 2, 0], "class": 3},
    {"num": 62, "name": "シロカベ", "furigana": "しろかべ", "bs": [40, 32, 17, 45, 30], "growPat": [1, 2, 0, 1, 0], "class": 3},
    {"num": 63, "name": "ヨロイさん", "furigana": "よろいさん", "bs": [35, 26, 20, 27, 21], "growPat": [2, 0, 3, 2, 0], "class": 3},
    {"num": 64, "name": "モレゾウ", "furigana": "もれぞう", "bs": [33, 29, 22, 29, 26], "growPat": [0, 0, 3, 2, 0], "class": 3},
    {"num": 65, "name": "プルファント", "furigana": "ぷるふぁんと", "bs": [30, 26, 29, 31, 23], "growPat": [0, 0, 0, 2, 3], "class": 3},
    {"num": 66, "name": "トオセンボン", "furigana": "とおせんぼん", "bs": [34, 25, 18, 30, 20], "growPat": [2, 0, 2, 2, 3], "class": 3},
    {"num": 67, "name": "くろがねセンボン", "furigana": "くろがねせんぼん", "bs": [31, 28, 15, 28, 20], "growPat": [2, 2, 0, 2, 3], "class": 3},
    {"num": 68, "name": "ふじのやま", "furigana": "ふじのやま", "bs": [36, 28, 21, 29, 25], "growPat": [0, 2, 3, 2, 0], "class": 3},
    {"num": 69, "name": "さくらのじま", "furigana": "さくらのじま", "bs": [39, 32, 20, 30, 28], "growPat": [0, 2, 3, 2, 0], "class": 3},
    {"num": 70, "name": "グレるりん", "furigana": "ぐれるりん", "bs": [25, 25, 18, 21, 26], "growPat": [2, 2, 3, 0, 0], "class": 3},
    {"num": 71, "name": "ゴクドー", "furigana": "ごくどー", "bs": [34, 37, 16, 27, 30], "growPat": [2, 2, 3, 0, 0], "class": 3},
    {"num": 72, "name": "アニ鬼", "furigana": "あにき", "bs": [38, 40, 20, 30, 33], "growPat": [1, 1, 0, 2, 2], "class": 3},
    {"num": 73, "name": "武者かぶと", "furigana": "むしゃかぶと", "bs": [38, 32, 18, 33, 21], "growPat": [0, 2, 3, 2, 3], "class": 3},
    {"num": 74, "name": "かぶと無双", "furigana": "かぶとむそう", "bs": [38, 37, 21, 36, 30], "growPat": [0, 2, 3, 2, 3], "class": 3},
    {"num": 75, "name": "オオツノノ神", "furigana": "おおつののかみ", "bs": [43, 40, 17, 40, 34], "growPat": [2, 2, 3, 2, 0], "class": 3},
    {"num": 76, "name": "ドウカク", "furigana": "どうかく", "bs": [33, 26, 21, 30, 24], "growPat": [0, 0, 3, 2, 0], "class": 3},
    {"num": 77, "name": "ギンカク", "furigana": "ぎんかく", "bs": [35, 27, 19, 32, 27], "growPat": [0, 0, 3, 2, 0], "class": 3},
    {"num": 78, "name": "キンカク", "furigana": "きんかく", "bs": [39, 27, 20, 36, 30], "growPat": [0, 0, 3, 2, 0], "class": 3},
    {"num": 79, "name": "プラチナカク", "furigana": "ぷらちなかく", "bs": [45, 34, 23, 37, 33], "growPat": [1, 2, 0, 1, 0], "class": 3},
    {"num": 80, "name": "ロボニャン", "furigana": "ろぼにゃん", "bs": [39, 32, 24, 35, 34], "growPat": [1, 2, 0, 1, 2], "class": 3},
    {"num": 81, "name": "ゴルニャン", "furigana": "ごるにゃん", "bs": [44, 33, 21, 37, 39], "growPat": [1, 2, 0, 1, 2], "class": 3},
    {"num": 82, "name": "だいだらぼっち", "furigana": "だいだらぼっち", "bs": [44, 31, 22, 48, 27], "growPat": [2, 0, 3, 2, 3], "class": 3},
    {"num": 83, "name": "うみぼうず", "furigana": "うみぼうず", "bs": [45, 18, 34, 47, 30], "growPat": [2, 2, 2, 1, 0], "class": 3},
    {"num": 84, "name": "しゃれこ婦人", "furigana": "しゃれこふじん", "bs": [26, 24, 16, 25, 26], "growPat": [0, 2, 3, 2, 0], "class": 4},
    {"num": 85, "name": "カラカラさん", "furigana": "からからさん", "bs": [32, 27, 23, 29, 28], "growPat": [0, 2, 3, 2, 0], "class": 4},
    {"num": 86, "name": "ほね美人", "furigana": "ほねびじん", "bs": [35, 29, 26, 32, 32], "growPat": [2, 2, 0, 1, 1], "class": 4},
    {"num": 87, "name": "セミまる", "furigana": "せみまる", "bs": [24, 25, 21, 19, 26], "growPat": [2, 2, 3, 3, 2], "class": 4},
    {"num": 88, "name": "カゲまる", "furigana": "かげまる", "bs": [32, 33, 27, 21, 31], "growPat": [2, 2, 3, 3, 2], "class": 4},
    {"num": 89, "name": "ヒグラシまる", "furigana": "ひぐらしまる", "bs": [35, 34, 29, 20, 36], "growPat": [2, 2, 2, 0, 2], "class": 4},
    {"num": 90, "name": "さむガリ", "furigana": "さむがり", "bs": [24, 23, 25, 19, 24], "growPat": [0, 3, 2, 0, 2], "class": 4},
    {"num": 91, "name": "ガリ王子", "furigana": "がりおうじ", "bs": [32, 37, 34, 26, 33], "growPat": [0, 3, 2, 0, 2], "class": 4},
    {"num": 92, "name": "あつガルル", "furigana": "あつがるる", "bs": [36, 35, 38, 30, 35], "growPat": [2, 2, 1, 0, 1], "class": 4},
    {"num": 93, "name": "ジバニャン", "furigana": "じばにゃん", "bs": [30, 20, 19, 24, 35], "growPat": [0, 2, 3, 0, 2], "class": 4},
    {"num": 94, "name": "トゲニャン", "furigana": "とげにゃん", "bs": [33, 24, 20, 27, 38], "growPat": [0, 2, 3, 0, 2], "class": 4},
    {"num": 95, "name": "ワルニャン", "furigana": "わるにゃん", "bs": [35, 33, 15, 30, 36], "growPat": [0, 2, 3, 0, 2], "class": 4},
    {"num": 96, "name": "ノガッパ", "furigana": "のがっぱ", "bs": [28, 22, 29, 22, 28], "growPat": [0, 0, 2, 3, 2], "class": 4},
    {"num": 97, "name": "たびガッパ", "furigana": "たびがっぱ", "bs": [38, 37, 29, 24, 31], "growPat": [0, 0, 2, 3, 2], "class": 4},
    {"num": 98, "name": "なみガッパ", "furigana": "なみがっぱ", "bs": [39, 34, 34, 25, 32], "growPat": [2, 2, 1, 0, 1], "class": 4},
    {"num": 99, "name": "コマさん", "furigana": "こまさん", "bs": [25, 22, 30, 23, 27], "growPat": [2, 3, 2, 0, 2], "class": 4},
    {"num": 100, "name": "ししコマ", "furigana": "ししこま", "bs": [35, 26, 39, 23, 36], "growPat": [2, 3, 2, 0, 2], "class": 4},
    {"num": 101, "name": "コマじろう", "furigana": "こまじろう", "bs": [25, 26, 27, 24, 25], "growPat": [2, 0, 0, 0, 2], "class": 4},
    {"num": 102, "name": "とらじろう", "furigana": "とらじろう", "bs": [31, 30, 34, 24, 33], "growPat": [2, 0, 0, 0, 2], "class": 4},
    {"num": 103, "name": "バク", "furigana": "ばく", "bs": [24, 22, 28, 23, 27], "growPat": [2, 3, 2, 0, 2], "class": 4},
    {"num": 104, "name": "ハク", "furigana": "はく", "bs": [26, 25, 31, 26, 29], "growPat": [2, 3, 2, 2, 0], "class": 4},
    {"num": 105, "name": "キュン太郎", "furigana": "きゅんたろう", "bs": [26, 22, 31, 24, 29], "growPat": [0, 3, 2, 3, 2], "class": 4},
    {"num": 106, "name": "ズキュキュン太", "furigana": "ずきゅきゅんた", "bs": [31, 22, 39, 26, 36], "growPat": [0, 3, 2, 3, 2], "class": 4},
    {"num": 107, "name": "裏キュン太", "furigana": "うらきゅんた", "bs": [35, 28, 34, 29, 38], "growPat": [2, 2, 2, 2, 2], "class": 4},
    {"num": 108, "name": "ゆきおんな", "furigana": "ゆきおんな", "bs": [32, 22, 37, 25, 33], "growPat": [0, 0, 2, 3, 0], "class": 4},
    {"num": 109, "name": "ふぶき姫", "furigana": "ふぶきひめ", "bs": [36, 27, 43, 29, 34], "growPat": [0, 0, 2, 3, 0], "class": 4},
    {"num": 110, "name": "百鬼姫", "furigana": "ひゃっきひめ", "bs": [39, 33, 41, 31, 30], "growPat": [0, 0, 2, 3, 0], "class": 4},
    {"num": 111, "name": "ワカメくん", "furigana": "わかめくん", "bs": [24, 22, 26, 20, 27], "growPat": [2, 2, 3, 0, 0], "class": 5},
    {"num": 112, "name": "コンブさん", "furigana": "こんぶさん", "bs": [26, 20, 24, 24, 25], "growPat": [2, 3, 0, 2, 0], "class": 5},
    {"num": 113, "name": "メカブちゃん", "furigana": "めかぶちゃん", "bs": [28, 19, 28, 23, 31], "growPat": [0, 0, 2, 3, 2], "class": 5},
    {"num": 114, "name": "うらやましろう", "furigana": "うらやましろう", "bs": [26, 25, 23, 20, 26], "growPat": [2, 2, 0, 3, 2], "class": 5},
    {"num": 115, "name": "つまみぐいのすけ", "furigana": "つまみぐいのすけ", "bs": [29, 28, 23, 21, 28], "growPat": [2, 2, 0, 3, 2], "class": 5},
    {"num": 116, "name": "ひも爺", "furigana": "ひもじい", "bs": [25, 22, 26, 20, 24], "growPat": [0, 0, 2, 3, 0], "class": 5},
    {"num": 117, "name": "くいい爺", "furigana": "くいいじい", "bs": [32, 35, 30, 26, 26], "growPat": [0, 0, 2, 3, 0], "class": 5},
    {"num": 118, "name": "こめ爺", "furigana": "こめじい", "bs": [34, 28, 37, 26, 29], "growPat": [2, 2, 1, 0, 2], "class": 5},
    {"num": 119, "name": "びんボーイ", "furigana": "びんぼーい", "bs": [27, 22, 29, 24, 27], "growPat": [2, 2, 0, 2, 0], "class": 5},
    {"num": 120, "name": "らくてん童", "furigana": "らくてんどう", "bs": [29, 25, 27, 27, 29], "growPat": [2, 0, 2, 0, 2], "class": 5},
    {"num": 121, "name": "ぜっこう蝶", "furigana": "ぜっこうちょう", "bs": [25, 24, 28, 21, 29], "growPat": [0, 3, 2, 3, 2], "class": 5},
    {"num": 122, "name": "ゼッコウ蝶", "furigana": "ぜっこうちょう", "bs": [26, 20, 30, 22, 26], "growPat": [0, 3, 2, 3, 2], "class": 5},
    {"num": 123, "name": "サイコウ蝶", "furigana": "さいこうちょう", "bs": [35, 25, 39, 25, 35], "growPat": [0, 3, 2, 3, 2], "class": 5},
    {"num": 124, "name": "アゲアゲハ", "furigana": "あげあげは", "bs": [39, 27, 42, 27, 39], "growPat": [2, 2, 2, 2, 2], "class": 5},
    {"num": 125, "name": "ホノボーノ", "furigana": "ほのぼーの", "bs": [27, 23, 29, 25, 28], "growPat": [0, 3, 2, 0, 2], "class": 5},
    {"num": 126, "name": "ヒョウヘンヌ", "furigana": "ひょうへんぬ", "bs": [31, 29, 36, 29, 32], "growPat": [0, 3, 2, 0, 2], "class": 5},
    {"num": 127, "name": "ヒョウヘンナ", "furigana": "ひょうへんな", "bs": [35, 32, 37, 26, 34], "growPat": [2, 2, 1, 2, 2], "class": 5},
    {"num": 128, "name": "サンタク老師", "furigana": "さんたくろうし", "bs": [28, 27, 30, 26, 31], "growPat": [2, 0, 0, 0, 2], "class": 5},
    {"num": 129, "name": "ふくろじじい", "furigana": "ふくろじじい", "bs": [31, 26, 30, 28, 27], "growPat": [2, 0, 2, 0, 0], "class": 5},
    {"num": 130, "name": "さいの目入道", "furigana": "さいのめにゅうどう", "bs": [33, 31, 31, 31, 31], "growPat": [0, 2, 3, 2, 0], "class": 5},
    {"num": 131, "name": "ゾロ目大明神", "furigana": "ぞろめだいみょうじん", "bs": [36, 28, 28, 32, 33], "growPat": [2, 3, 0, 0, 2], "class": 5},
    {"num": 132, "name": "雷オトン", "furigana": "らいおとん", "bs": [33, 32, 29, 30, 35], "growPat": [2, 2, 3, 3, 0], "class": 5},
    {"num": 133, "name": "絶オジイ", "furigana": "ぜつおじい", "bs": [39, 37, 31, 31, 36], "growPat": [1, 1, 0, 2, 0], "class": 5},
    {"num": 134, "name": "聖オカン", "furigana": "せいおかん", "bs": [36, 29, 36, 28, 38], "growPat": [0, 3, 2, 3, 2], "class": 5},
    {"num": 135, "name": "心オバア", "furigana": "しんおばあ", "bs": [39, 31, 38, 27, 39], "growPat": [0, 3, 2, 3, 2], "class": 5},
    {"num": 136, "name": "みちび鬼", "furigana": "みちびき", "bs": [26, 26, 18, 25, 24], "growPat": [2, 2, 3, 2, 2], "class": 6},
    {"num": 137, "name": "えこひい鬼", "furigana": "えこひいき", "bs": [28, 29, 19, 27, 26], "growPat": [2, 2, 3, 2, 2], "class": 6},
    {"num": 138, "name": "かたのり小僧", "furigana": "かたのりこぞう", "bs": [24, 25, 22, 20, 26], "growPat": [0, 2, 0, 3, 2], "class": 6},
    {"num": 139, "name": "かたのり親方", "furigana": "かたのりおやかた", "bs": [32, 32, 23, 25, 27], "growPat": [0, 2, 0, 3, 2], "class": 6},
    {"num": 140, "name": "ぎっくり男", "furigana": "ぎっくりおとこ", "bs": [35, 36, 23, 27, 33], "growPat": [1, 1, 2, 0, 2], "class": 6},
    {"num": 141, "name": "ネガティブーン", "furigana": "ねがてぃぶーん", "bs": [23, 26, 20, 22, 25], "growPat": [0, 2, 3, 0, 2], "class": 6},
    {"num": 142, "name": "ジャネガブーン", "furigana": "じゃねがぶーん", "bs": [31, 29, 22, 24, 33], "growPat": [0, 2, 3, 0, 2], "class": 6},
    {"num": 143, "name": "かゆかゆ", "furigana": "かゆかゆ", "bs": [37, 34, 27, 29, 37], "growPat": [2, 1, 0, 2, 1], "class": 6},
    {"num": 144, "name": "ジミー", "furigana": "じみー", "bs": [24, 24, 23, 18, 28], "growPat": [0, 2, 0, 3, 2], "class": 6},
    {"num": 145, "name": "カゲロー", "furigana": "かげろー", "bs": [31, 32, 31, 20, 35], "growPat": [0, 2, 0, 3, 2], "class": 6},
    {"num": 146, "name": "カイム", "furigana": "かいむ", "bs": [33, 35, 34, 20, 37], "growPat": [0, 2, 0, 3, 2], "class": 6},
    {"num": 147, "name": "ヒキコウモリ", "furigana": "ひきこうもり", "bs": [27, 22, 26, 29, 25], "growPat": [2, 3, 0, 3, 2], "class": 6},
    {"num": 148, "name": "トジコウモリ", "furigana": "とじこうもり", "bs": [29, 30, 31, 37, 30], "growPat": [2, 3, 0, 3, 2], "class": 6},
    {"num": 149, "name": "ヤドコウモリ", "furigana": "やどこうもり", "bs": [30, 31, 32, 40, 31], "growPat": [1, 0, 2, 0, 1], "class": 6},
    {"num": 150, "name": "ぎしんあん鬼", "furigana": "ぎしんあんき", "bs": [20, 25, 26, 26, 27], "growPat": [0, 0, 2, 0, 2], "class": 6},
    {"num": 151, "name": "ガ鬼", "furigana": "がき", "bs": [22, 29, 19, 29, 25], "growPat": [2, 2, 3, 2, 0], "class": 6},
    {"num": 152, "name": "あまのじゃく", "furigana": "あまのじゃく", "bs": [25, 28, 33, 31, 29], "growPat": [2, 0, 2, 2, 3], "class": 6},
    {"num": 153, "name": "ネクラマテング", "furigana": "ねくらまてんぐ", "bs": [28, 21, 32, 23, 30], "growPat": [2, 3, 2, 0, 2], "class": 6},
    {"num": 154, "name": "ヨミテング", "furigana": "よみてんぐ", "bs": [30, 23, 34, 25, 32], "growPat": [0, 3, 2, 3, 1], "class": 6},
    {"num": 155, "name": "魔ガサス", "furigana": "まがさす", "bs": [30, 23, 39, 28, 39], "growPat": [2, 0, 2, 0, 1], "class": 6},
    {"num": 156, "name": "ウ魔", "furigana": "うま", "bs": [36, 31, 28, 33, 36], "growPat": [2, 2, 2, 0, 2], "class": 6},
    {"num": 157, "name": "デビビル", "furigana": "でびびる", "bs": [26, 37, 29, 33, 32], "growPat": [2, 2, 3, 3, 0], "class": 6},
    {"num": 158, "name": "デビビラン", "furigana": "でびびらん", "bs": [28, 33, 31, 35, 35], "growPat": [2, 2, 3, 3, 0], "class": 6},
    {"num": 159, "name": "虫歯伯爵", "furigana": "むしばはくしゃく", "bs": [44, 35, 33, 23, 39], "growPat": [2, 2, 3, 3, 0], "class": 6},
    {"num": 160, "name": "ドケチング", "furigana": "どけちんぐ", "bs": [37, 27, 41, 32, 35], "growPat": [0, 3, 2, 3, 0], "class": 6},
    {"num": 161, "name": "ヤミまろ", "furigana": "やみまろ", "bs": [40, 29, 39, 34, 32], "growPat": [2, 0, 1, 0, 2], "class": 6},
    {"num": 162, "name": "イガイガグリ", "furigana": "いがいがぐり", "bs": [28, 23, 17, 25, 26], "growPat": [2, 0, 0, 2, 2], "class": 7},
    {"num": 163, "name": "チクチクウニ", "furigana": "ちくちくうに", "bs": [30, 25, 16, 26, 25], "growPat": [2, 2, 0, 2, 0], "class": 7},
    {"num": 164, "name": "ヨコドリ", "furigana": "よこどり", "bs": [25, 18, 26, 28, 22], "growPat": [2, 3, 2, 2, 0], "class": 7},
    {"num": 165, "name": "はらおドリ", "furigana": "はらおどり", "bs": [28, 20, 27, 30, 24], "growPat": [2, 3, 2, 2, 0], "class": 7},
    {"num": 166, "name": "トホホギス", "furigana": "とほほぎす", "bs": [26, 19, 27, 22, 25], "growPat": [0, 0, 2, 3, 2], "class": 7},
    {"num": 167, "name": "ブカッコウ", "furigana": "ぶかっこう", "bs": [30, 22, 32, 24, 31], "growPat": [0, 0, 2, 3, 2], "class": 7},
    {"num": 168, "name": "死神鳥", "furigana": "しにがみどり", "bs": [38, 32, 32, 28, 34], "growPat": [2, 2, 1, 2, 2], "class": 7},
    {"num": 169, "name": "じんめん犬", "furigana": "じんめんけん", "bs": [24, 25, 23, 20, 25], "growPat": [0, 2, 0, 3, 2], "class": 7},
    {"num": 170, "name": "かおベロス", "furigana": "かおべろす", "bs": [32, 34, 26, 25, 30], "growPat": [0, 2, 0, 3, 2], "class": 7},
    {"num": 171, "name": "三途の犬", "furigana": "さんずのいぬ", "bs": [35, 38, 28, 30, 33], "growPat": [1, 1, 2, 0, 2], "class": 7},
    {"num": 172, "name": "じめりんぼう", "furigana": "じめりんぼう", "bs": [22, 23, 26, 24, 22], "growPat": [0, 0, 2, 0, 2], "class": 7},
    {"num": 173, "name": "雨ふらし", "furigana": "あめふらし", "bs": [28, 25, 30, 28, 31], "growPat": [0, 0, 2, 0, 2], "class": 7},
    {"num": 174, "name": "こおりんぼう", "furigana": "こおりんぼう", "bs": [24, 22, 24, 26, 21], "growPat": [0, 0, 2, 2, 0], "class": 7},
    {"num": 175, "name": "化けあられ", "furigana": "ばけあられ", "bs": [30, 20, 31, 34, 27], "growPat": [0, 0, 2, 2, 0], "class": 7},
    {"num": 176, "name": "たらりん", "furigana": "たらりん", "bs": [27, 24, 29, 23, 31], "growPat": [2, 2, 1, 1, 2], "class": 7},
    {"num": 177, "name": "口だけおんな", "furigana": "くちだけおんな", "bs": [29, 23, 29, 21, 27], "growPat": [2, 0, 0, 3, 2], "class": 7},
    {"num": 178, "name": "おつぼね様", "furigana": "おつぼねさま", "bs": [31, 34, 20, 25, 29], "growPat": [2, 2, 3, 0, 2], "class": 7},
    {"num": 179, "name": "ドンヨリーヌ", "furigana": "どんよりーぬ", "bs": [30, 22, 26, 28, 23], "growPat": [2, 0, 2, 2, 3], "class": 7},
    {"num": 180, "name": "おならず者", "furigana": "おならずもの", "bs": [26, 27, 26, 23, 30], "growPat": [0, 0, 0, 3, 2], "class": 7},
    {"num": 181, "name": "へこ鬼神", "furigana": "へこきじん", "bs": [32, 33, 30, 28, 36], "growPat": [0, 0, 0, 3, 2], "class": 7},
    {"num": 182, "name": "おすべり様", "furigana": "おすべりさま", "bs": [32, 27, 32, 29, 37], "growPat": [2, 3, 0, 3, 2], "class": 7},
    {"num": 183, "name": "おしっしょう", "furigana": "おしっしょう", "bs": [37, 26, 28, 26, 35], "growPat": [2, 3, 0, 3, 2], "class": 7},
    {"num": 184, "name": "しわくちゃん", "furigana": "しわくちゃん", "bs": [32, 25, 35, 24, 33], "growPat": [0, 0, 3, 2, 0], "class": 7},
    {"num": 185, "name": "老いらん", "furigana": "おいらん", "bs": [40, 27, 40, 29, 36], "growPat": [0, 0, 3, 2, 0], "class": 7},
    {"num": 186, "name": "ふじみ御前", "furigana": "ふじみごぜん", "bs": [43, 26, 40, 27, 38], "growPat": [0, 0, 3, 2, 0], "class": 7},
    {"num": 187, "name": "フゥミン", "furigana": "ふぅみん", "bs": [35, 26, 36, 27, 38], "growPat": [2, 3, 2, 3, 2], "class": 7},
    {"num": 188, "name": "おねむの精", "furigana": "おねむのせい", "bs": [39, 28, 38, 29, 40], "growPat": [1, 0, 1, 0, 1], "class": 7},
    {"num": 189, "name": "ツチノコ", "furigana": "つちのこ", "bs": [29, 15, 25, 16, 39], "growPat": [1, 3, 1, 0, 1], "class": 8},
    {"num": 190, "name": "フクリュウ", "furigana": "ふくりゅう", "bs": [33, 26, 31, 29, 46], "growPat": [1, 3, 1, 0, 1], "class": 8},
    {"num": 191, "name": "ツチノコパンダ", "furigana": "つちのこぱんだ", "bs": [35, 28, 31, 31, 41], "growPat": [2, 2, 2, 2, 2], "class": 8},
    {"num": 192, "name": "笑ウツボ", "furigana": "わらうつぼ", "bs": [27, 30, 21, 25, 24], "growPat": [0, 2, 3, 2, 0], "class": 8},
    {"num": 193, "name": "歌ウナギ", "furigana": "うたうなぎ", "bs": [29, 31, 24, 26, 27], "growPat": [0, 0, 2, 3, 2], "class": 8},
    {"num": 194, "name": "大蛇のツボ", "furigana": "だいじゃのつぼ", "bs": [30, 34, 21, 28, 24], "growPat": [2, 2, 3, 0, 0], "class": 8},
    {"num": 195, "name": "キライギョ", "furigana": "きらいぎょ", "bs": [27, 23, 26, 25, 21], "growPat": [2, 0, 2, 0, 3], "class": 8},
    {"num": 196, "name": "ゾウオ", "furigana": "ぞうお", "bs": [33, 29, 36, 31, 20], "growPat": [2, 0, 2, 0, 3], "class": 8},
    {"num": 197, "name": "ギャクジョウオ", "furigana": "ぎゃくじょうお", "bs": [37, 36, 31, 35, 25], "growPat": [2, 1, 2, 2, 2], "class": 8},
    {"num": 198, "name": "りゅーくん", "furigana": "りゅーくん", "bs": [30, 22, 28, 23, 26], "growPat": [2, 0, 0, 0, 0], "class": 8},
    {"num": 199, "name": "龍神", "furigana": "りゅうじん", "bs": [35, 30, 32, 29, 36], "growPat": [2, 0, 0, 0, 0], "class": 8},
    {"num": 200, "name": "青龍", "furigana": "せいりゅう", "bs": [40, 38, 32, 31, 33], "growPat": [2, 1, 0, 2, 2], "class": 8},
    {"num": 201, "name": "ボー坊", "furigana": "ぼーぼう", "bs": [32, 15, 33, 27, 36], "growPat": [2, 2, 2, 2, 2], "class": 8},
    {"num": 202, "name": "しどろもどろ", "furigana": "しどろもどろ", "bs": [29, 26, 35, 30, 27], "growPat": [0, 0, 0, 0, 0], "class": 8},
    {"num": 203, "name": "ミチクサメ", "furigana": "みちくさめ", "bs": [33, 32, 20, 26, 28], "growPat": [2, 2, 3, 0, 0], "class": 8},
    {"num": 204, "name": "トーシロザメ", "furigana": "とーしろざめ", "bs": [37, 35, 18, 31, 25], "growPat": [2, 2, 3, 2, 3], "class": 8},
    {"num": 205, "name": "ムダヅカイ", "furigana": "むだづかい", "bs": [30, 22, 25, 34, 23], "growPat": [2, 3, 0, 2, 3], "class": 8},
    {"num": 206, "name": "はらわシェル", "furigana": "はらわしぇる", "bs": [33, 24, 28, 43, 26], "growPat": [1, 0, 2, 1, 0], "class": 8},
    {"num": 207, "name": "ナガバナ", "furigana": "ながばな", "bs": [32, 31, 19, 27, 30], "growPat": [2, 0, 3, 0, 0], "class": 8},
    {"num": 208, "name": "ナガバナナ", "furigana": "ながばなな", "bs": [39, 41, 21, 32, 31], "growPat": [2, 0, 3, 0, 0], "class": 8},
    {"num": 209, "name": "しきるん蛇", "furigana": "しきるんじゃ", "bs": [35, 28, 24, 27, 35], "growPat": [2, 0, 0, 3, 2], "class": 8},
    {"num": 210, "name": "すねスネーク", "furigana": "すねすねーく", "bs": [33, 20, 33, 30, 28], "growPat": [2, 3, 2, 0, 0], "class": 8},
    {"num": 211, "name": "まむし行司", "furigana": "まむしぎょうじ", "bs": [36, 32, 22, 33, 29], "growPat": [0, 2, 3, 2, 0], "class": 8},
    {"num": 212, "name": "オロチ", "furigana": "おろち", "bs": [35, 32, 38, 25, 42], "growPat": [0, 0, 2, 0, 2], "class": 8},
    {"num": 213, "name": "影オロチ", "furigana": "かげおろち", "bs": [38, 39, 33, 29, 35], "growPat": [0, 0, 2, 0, 2], "class": 8},
    {"num": 214, "name": "ブシニャン", "furigana": "ぶしにゃん", "bs": [31, 39, 27, 23, 50], "growPat": [2, 2, 0, 0, 1], "class": 1},
    {"num": 215, "name": "しゅらコマ", "furigana": "しゅらこま", "bs": [37, 25, 50, 27, 39], "growPat": [2, 0, 1, 0, 2], "class": 2},
    {"num": 216, "name": "イケメン犬", "furigana": "いけめんけん", "bs": [43, 20, 40, 26, 47], "growPat": [2, 2, 2, 0, 2], "class": 7},
    {"num": 217, "name": "花さか爺", "furigana": "はなさかじい", "bs": [32, 29, 41, 31, 45], "growPat": [2, 0, 2, 2, 2], "class": 5},
    {"num": 218, "name": "山吹鬼", "furigana": "やまぶきおに", "bs": [45, 50, 18, 35, 40], "growPat": [1, 1, 0, 0, 0], "class": 3},
    {"num": 219, "name": "サファイニャン", "furigana": "さふぁいにゃん", "bs": [33, 28, 38, 28, 36], "growPat": [2, 0, 1, 2, 2], "class": 4},
    {"num": 220, "name": "エメラルニャン", "furigana": "えめらるにゃん", "bs": [31, 30, 34, 30, 38], "growPat": [2, 2, 2, 0, 1], "class": 4},
    {"num": 221, "name": "ルビーニャン", "furigana": "るびーにゃん", "bs": [38, 39, 27, 27, 32], "growPat": [2, 1, 0, 2, 2], "class": 4},
    {"num": 222, "name": "トパニャン", "furigana": "とぱにゃん", "bs": [41, 31, 28, 35, 28], "growPat": [2, 2, 2, 1, 0], "class": 4},
    {"num": 223, "name": "ダイヤニャン", "furigana": "だいやにゃん", "bs": [35, 33, 30, 31, 34], "growPat": [2, 2, 2, 2, 2], "class": 4},
    {"num": 224, "name": "ミツマタノヅチ", "furigana": "みつまたのづち", "bs": [400, 12, 10, 0, 10], "growPat": [5, 5, 5, 5, 5], "class": 0},
    {"num": 225, "name": "のぼせトンマン", "furigana": "のぼせとんまん", "bs": [900, 37, 32, 20, 33], "growPat": [5, 5, 5, 5, 5], "class": 0},
    {"num": 226, "name": "つられたろう丸", "furigana": "つられたろうまる", "bs": [1100, 43, 37, 23, 38], "growPat": [5, 5, 5, 5, 5], "class": 0},
    {"num": 227, "name": "おぼろ入道", "furigana": "おぼろにゅうどう", "bs": [1800, 75, 45, 25, 40], "growPat": [5, 5, 5, 5, 5], "class": 0},
    {"num": 228, "name": "亡霊武者", "furigana": "ぼうれいむしゃ", "bs": [1200, 64, 52, 5, 59], "growPat": [5, 5, 5, 5, 5], "class": 0},
    {"num": 229, "name": "鬼くももん", "furigana": "おにくももん", "bs": [2400, 94, 82, 5, 71], "growPat": [5, 5, 5, 5, 5], "class": 0},
    {"num": 230, "name": "やぶれかぶれ院長", "furigana": "やぶれかぶれいんちょう", "bs": [3000, 98, 101, 51, 95], "growPat": [5, 5, 5, 5, 5], "class": 0},
    {"num": 231, "name": "イカカモネ議長1", "furigana": "いかかもねぎちょう", "bs": [3800, 121, 113, 68, 102], "growPat": [5, 5, 5, 5, 5], "class": 0},
    {"num": 232, "name": "イカカモネ議長2", "furigana": "いかかもねぎちょう", "bs": [3800, 115, 112, 45, 106], "growPat": [5, 5, 5, 5, 5], "class": 0},
    {"num": 233, "name": "あおべえあかべえ", "furigana": "あおべえあかべえ", "bs": [4500, 105, 112, 112, 121], "growPat": [5, 5, 5, 5, 5], "class": 0},
    {"num": 234, "name": "つづらチュン太夫", "furigana": "つづらちゅんだゆう", "bs": [3500, 124, 147, 121, 128], "growPat": [5, 5, 5, 5, 5], "class": 0},
    {"num": 235, "name": "地獄大山椒", "furigana": "じごくおおさんしょう", "bs": [4400, 15, 10, 10, 120], "growPat": [5, 5, 5, 5, 5], "class": 0},
    {"num": 236, "name": "おのぼり黒トン", "furigana": "おのぼりくろとん", "bs": [4700, 226, 300, 197, 128], "growPat": [5, 5, 5, 5, 5], "class": 0},
    {"num": 237, "name": "第八三途丸", "furigana": "だいはちさんずまる", "bs": [4500, 215, 230, 121, 150], "growPat": [5, 5, 5, 5, 5], "class": 0},
    {"num": 238, "name": "月影入道", "furigana": "つきかげにゅうどう", "bs": [4900, 256, 230, 136, 168], "growPat": [5, 5, 5, 5, 5], "class": 0},
    {"num": 239, "name": "キモナシ先生", "furigana": "きもなしせんせい", "bs": [4500, 206, 210, 134, 224], "growPat": [5, 5, 5, 5, 5], "class": 0},
    {"num": 240, "name": "国宝・影正", "furigana": "こくほうかげまさ", "bs": [4000, 265, 200, 5, 185], "growPat": [5, 5, 5, 5, 5], "class": 0},
    {"num": 241, "name": "ゲソヒゲール大臣", "furigana": "げそひげーるだいじん", "bs": [4600, 210, 190, 75, 186], "growPat": [5, 5, 5, 5, 5], "class": 0},
    {"num": 242, "name": "どんどろ", "furigana": "どんどろ", "bs": [5000, 195, 195, 141, 145], "growPat": [5, 5, 5, 5, 5], "class": 0},
    {"num": 243, "name": "赤鬼", "furigana": "あかおに", "bs": [6000, 270, 270, 150, 270], "growPat": [5, 5, 5, 5, 5], "class": 0},
    {"num": 244, "name": "青鬼", "furigana": "あおおに", "bs": [8000, 300, 360, 170, 320], "growPat": [5, 5, 5, 5, 5], "class": 0},
    {"num": 245, "name": "黒鬼", "furigana": "くろおに", "bs": [9900, 450, 430, 190, 340], "growPat": [5, 5, 5, 5, 5], "class": 0}
];

characteristicsData = [
  // H A M D S
  {"name": "なし", "bonus": [0,0,0,0,0]},
  {"name": "手動入力", "bonus": [], "inputManually": true},
  {"name": "短気", "bonus": [10,10,0,0,0]},
  {"name": "荒くれ", "bonus": [0,20,0,0,0]},
  {"name": "れいせい", "bonus": [0,0,10,0,10]},
  {"name": "ずのう的", "bonus": [0,0,20,0,0]},
  {"name": "しんちょう", "bonus": [0,0,10,10,0]},
  {"name": "動じない", "bonus": [0,0,0,20,0]},
  {"name": "やさしい", "bonus": [10,0,10,0,0]},
  {"name": "情け深い", "bonus": [20,0,0,0,0]},
  {"name": "いやらしい", "bonus": [0,10,0,0,10]},
  {"name": "非道", "bonus": [0,0,0,0,20]},
  {"name": "協力的", "bonus": [10,0,0,0,10]},
  {"name": "けんしん的", "bonus": [0,10,0,10,0]},
]

growPatName = ["普通", "早熟", "やや早熟", "やや晩成", "晩成", "ボス"];
classes = [
  // H A M D S
  {"name": "ボス", "bs_A": [false, false, false, false, false]},
  {"name": "イサマシ", "bs_A": [false, true, false, false, false]},
  {"name": "フシギ", "bs_A": [false, false, true, false, false]},
  {"name": "ゴーケツ", "bs_A": [false, false, false, true, false]},
  {"name": "プリチー", "bs_A": [false, false, false, false, true]},
  {"name": "ポカポカ", "bs_A": [false, false, true, true, false]},
  {"name": "ウスラカゲ", "bs_A": [true, false, false, false, true]},
  {"name": "ブキミー", "bs_A": [false, true, true, false, false]},
  {"name": "ニョロロン", "bs_A": [true, false, false, false, false]}
]
statusNames = [
  {"name": "H", "displayName": "HP"},
  {"name": "A", "displayName": "ちから"},
  {"name": "M", "displayName": "ようりょく"},
  {"name": "D", "displayName": "まもり"},
  {"name": "S", "displayName": "すばやさ"}
];
statusNamesDict = {
  "H": "HP",
  "A": "ちから",
  "M": "ようりょく",
  "D": "まもり",
  "S": "すばやさ"
};

floats = new Float32Array(24);
floats[0] = 1.0;  // = 3F800000
floats[1] = 9604.0; // = 46161000
floats[2] = 0.449999988079071044921875; // = 3EE66666
floats[3] = 0.25;  // = 3E800000
floats[4] = 0.010204081423580646514892578125;  // = 3C272F05
floats[5] = 98.0; // = 42C40000
floats[6] = 0.100000001490116119384765625;  // = 3DCCCCCD
floats[7] = 99.0; // = 42C60000
floats[8] = 0.5; // = 3F000000
floats[9] = 0.0050505050458014011383056640625;  // = 3BA57EB5
floats[10] = 0.0500000007450580596923828125; // = 3D4CCCCD

function sum(a) {
  return a.reduce(function(x, y) { return x + y; });
}

function calcStatus (growPattern, bs, ivA, ivB_1, ivB_2, cb, level, isHP) {
  floats[11] = level;
  floats[11] = floats[11] * floats[9];
  floats[11] = floats[11] + floats[0];
  floats[11] = floats[11] * cb;
  floats[23] = floats[11];
  floats[11] = bs + ivA + ivB_1;
  if (isHP) {
    floats[23] = floats[23] + floats[11];
  } else {
    floats[23] = floats[23] + bs * floats[6];
  }
  floats[11] = floats[11] + ivB_2; // (2) これに、growPatternに応じたレベルと0.1か0.05をかける
  if (isHP) {
    floats[11] = floats[11] * floats[6];
  } else {
    floats[11] = floats[11] * floats[10];
  }
  if (growPattern === 0) {
    floats[11] = floats[11] * level;
    floats[23] = floats[23] + floats[11];
  } else if (growPattern === 1) {
    floats[12] = level - floats[0];
    floats[12] = floats[5] - floats[12];
    floats[13] = floats[7] - (floats[12] * floats[12] * floats[12] * floats[8] / floats[1] +
      floats[12] * floats[12] * floats[3] * floats[4] + floats[12] * floats[3]);
    floats[11] = floats[11] * floats[13];
    floats[23] = floats[23] + floats[11];
  } else if (growPattern === 2) {
    floats[12] = level - floats[0];
    floats[12] = floats[5] - floats[12];
    floats[13] = floats[7] - (floats[12] * floats[12] * floats[12] * floats[6] / floats[1] +
      floats[12] * floats[12] * floats[2] * floats[4] + floats[12] * floats[2]);
    floats[11] = floats[11] * floats[13];
    floats[23] = floats[23] + floats[11];
  } else if (growPattern === 3) {
    floats[12] = level - floats[0];
    floats[13] = floats[0] + (floats[12] * floats[12] * floats[12] * floats[6] / floats[1] +
      floats[12] * floats[12] * floats[2] * floats[4] + floats[12] * floats[2]);
    floats[11] = floats[11] * floats[13];
    floats[23] = floats[23] + floats[11];
  } else if (growPattern === 4) {
    floats[12] = level - floats[0];
    floats[13] = floats[0] + (floats[12] * floats[12] * floats[12] * floats[8] / floats[1] +
      floats[12] * floats[12] * floats[3] * floats[4] + floats[12] * floats[3]);
    floats[11] = floats[11] * floats[13];
    floats[23] = floats[23] + floats[11];
  } else if (growPattern === 5) {
    floats[23] = bs;
  } else {
    return 0;
  }
  return Math.floor(floats[23]);
};

function updateIVAFilter() {
  var yokai = {};
  var yokaiName = "";
  var $input_yokai_select = $("#input_yokai_select");
  var i = 0;

  if ($input_yokai_select.val() === "") {
    return false;
  }
  $.each(yokaiData, function (i) {
    if (yokaiData[i].name === $input_yokai_select.val()) {
      yokai = yokaiData[i];
      yokaiName = yokai.name;
      return false;
    }
  });
  if (yokaiName === "") {
    return false;
  }

  for (i = 0; i < statusNames.length; i++) {
    if (classes[yokai.class].bs_A[i]) {
      $("#input_yokai_iv_a_" + statusNames[i].name).prop("disabled", false);
    } else {
      $("#input_yokai_iv_a_" + statusNames[i].name).val("0");
      $("#input_yokai_iv_a_" + statusNames[i].name).prop("disabled", true);
    }
  }
}

function logPush(name, level, bs, iva, ivb_1, ivb_2, cb, st) {
  var $t = $("<table>");
  var $out = $("#result_log_area");
  var list = [
    {"name": name + " (" + level.toString(10) + ")", "value": []},
    {"name": "BS", "value": bs},
    {"name": "IVA", "value": iva},
    {"name": "IVB_1", "value": ivb_1},
    {"name": "IVB_2", "value": ivb_2},
    {"name": "CB", "value": cb},
    {"name": "ステ", "value": st},
  ]
  $.each(list, function(i, v) {
    var $tr = $("<tr>");
    $tr.append($("<th>").text(v.name).attr("colspan", 5 - v.value.length + 1));
    if (v.value.length > 0) {
      $.each(v.value, function(i, v) {
        var $td = $("<td>");
        $td.text(v);
        $tr.append($td);
      });
    }
    $t.append($tr);
  });
  $out.prepend($t);
  $out.children("table").slice(10).remove();
}

function calculateStatus(pushlog) {
  "use strict";
  var $input_yokai_select = $("#input_yokai_select");
  var $result_text = $("#result_text");
  var $result_output_area = $("#result_output_area");
  var $input_level = $("#input_level");
  var $warnings = $("#warnings");
  var yokaiName = "";
  var yokai = {};
  var level = 0;
  var statuses = [];
  var ivA = [];
  var ivB_1 = [];
  var ivB_2 = [];
  var results = [];
  var i = 0, j = 0;
  var ivB_1_sum_max = 10;
  var cb_sum_max = 20;

  var characteristics = $("#input_characteristics").val();
  var characteristicsBonus = [];

  $warnings.empty();
  $.each(characteristicsData, function (i) {
    if (characteristicsData[i].name === characteristics  && !characteristicsData[i].inputManually) {
      characteristicsBonus = characteristicsData[i].bonus.concat(); // copy an array
      $("table#manuallyInput input").each(function (j) {
        $(this).val(characteristicsBonus[j]);
      });
      return false;
    }
  });

  if ($input_yokai_select.val() === "") {
    $result_text.val("妖怪を指定してください．");
    return false;
  }
  $.each(yokaiData, function (i) {
    if (yokaiData[i].name === $input_yokai_select.val()) {
      yokai = yokaiData[i];
      yokaiName = yokai.name;
      return false;
    }
  });
  if (yokaiName === "") {
    $result_text.val("指定された妖怪は存在しません．");
    return false;
  }

  /* 種類が分かったら表を作成してしまう */
  $result_output_area.empty();
  $result_output_area.append("<div>").text(yokai.name).addClass("output_yokai_name");
  $result_output_area.append(
    $("<table>").append(
      $("<tr><td></td><th>H</th><th>A</th><th>M</th><th>D</th><th>S</th></tr>")
    ).append(
      $("<tr>").append(
        $("<th>").append("種族値")
      ).append(
        $("<td>").text(yokai.bs[0].toString(10))
      ).append(
        $("<td>").text(yokai.bs[1].toString(10))
      ).append(
        $("<td>").text(yokai.bs[2].toString(10))
      ).append(
        $("<td>").text(yokai.bs[3].toString(10))
      ).append(
        $("<td>").text(yokai.bs[4].toString(10))
      )
    ).append(
      $("<tr>").append(
        $("<th>").append("伸び")
      ).append(
        $("<td>").text(growPatName[yokai.growPat[0]])
      ).append(
        $("<td>").text(growPatName[yokai.growPat[1]])
      ).append(
        $("<td>").text(growPatName[yokai.growPat[2]])
      ).append(
        $("<td>").text(growPatName[yokai.growPat[3]])
      ).append(
        $("<td>").text(growPatName[yokai.growPat[4]])
      )
    )
  );

  /* トゲニャン */
  if (yokai.num === 94) {
    $(".container").css("background", "#eee url('../img/togenyan.png') no-repeat center bottom / 100%");
  } else {
    $(".container").css("background", "");
  }

  if ($input_level.val().match(/^[1-9][0-9]?$/)) {
  //if ($input_level.val().match(/^[1-9][0-9]*$/)) {
    level = parseInt($input_level.val(), 10);
  } else {
    $result_text.val("レベルを(正しく)入力してください");
    return false;
  }

  /* 手動入力の場合 */
  if (characteristicsBonus.length === 0) { // manually input
    $("table#manuallyInput input").each(function () {
      var $elm = $(this);
      var st = /^input_yokai_cb_manual_([HAMDS])$/.exec($elm.attr("id"))[1];
      if ($elm.val().match(/^[1-9][0-9]{0,2}$|^0$/)) {
        characteristicsBonus.push(parseInt($elm.val(), 10));
      } else if ($elm.val() === "") {
        //$elm.val("0");
        characteristicsBonus.push(0);
      } else {
        $result_text.val(statusNamesDict[st] + "(性格ボーナス)の値が不正です");
        return false;
      }
    });
  }
  if (characteristicsBonus.length !== 5) {
    return false;
  }

  for (i = 0; i < statusNames.length; i++) {
    if ($("#input_yokai_iv_a_" + statusNames[i].name).val().match(/^\+?[1-9][0-9]?$|^0$/)) {
      ivA.push(parseInt($("#input_yokai_iv_a_" + statusNames[i].name).val(), 10));
    } else if ($("#input_yokai_iv_a_" + statusNames[i].name).val() === "") {
      ivA.push(0);
      //$("#input_yokai_iv_a_" + i).val("0");
    } else {
      $result_text.val(statusNames[i].displayName + "(個体値A)の値が不正です");
      return false;
    }
  }

  for (i = 0; i < statusNames.length; i++) {
    if ($("#input_yokai_iv_b_1_" + statusNames[i].name).val().match(/^\+?[1-9][0-9]?$|^0$/)) {
      ivB_1.push(parseInt($("#input_yokai_iv_b_1_" + statusNames[i].name).val(), 10));
    } else if ($("#input_yokai_iv_b_1_" + statusNames[i].name).val() === "") {
      ivB_1.push(0);
      //$("#input_yokai_iv_b_1_" + i).val("0");
    } else {
      $result_text.val(statusNames[i].displayName + "(個体値B_1)の値が不正です");
      return false;
    }
  }

  for (i = 0; i < statusNames.length; i++) {
    if ($("#input_yokai_iv_b_2_" + statusNames[i].name).val().match(/^\+?[1-9][0-9]?$|^0$/)) {
      ivB_2.push(parseInt($("#input_yokai_iv_b_2_" + statusNames[i].name).val(), 10));
    } else if ($("#input_yokai_iv_b_2_" + statusNames[i].name).val() === "") {
      ivB_2.push(0);
      //$("#input_yokai_iv_b_2_" + i).val("0");
    } else {
      $result_text.val(statusNames[i].displayName + "(個体値B_2)の値が不正です");
      return false;
    }
  }

  /* 計算 */
  if (sum(ivB_1) !== ivB_1_sum_max) {
    $warnings.append($("<div>").text("個体値B_1の合計(" + sum(ivB_1).toString(10) + ")が" +
    ivB_1_sum_max.toString(10) + "ではありません。").addClass("warning"));
  }
  if (sum(characteristicsBonus) > cb_sum_max) {
    $warnings.append($("<div>").text("性格ボーナスの合計(" + sum(characteristicsBonus).toString(10) + ")は" +
    cb_sum_max.toString(10) + "を超えています。").addClass("warning"));
  }

  var st;
  for (i = 0; i < 5; i++) {
    // function calcStatus (growPattern, bs, ivA, ivB_1, ivB_2, cb, level, isHP) {
    st = calcStatus(yokai.growPat[i], yokai.bs[i], ivA[i], ivB_1[i], ivB_2[i], characteristicsBonus[i], level, i === 0);
    results.push(st);
  }

  var resultStr = (function (r) {
    var l = 0;
    var out = "";
    while (l < r.length) {
      out += r[l].toString(10) + "-";
      l += 1;
    }
    return out.substring(0, out.length - 1);
  }(results));
  $result_text.val(yokaiName + " (" + level.toString(10) + ") " + resultStr);
  if (pushlog === true) {
    logPush(yokaiName, level, yokai.bs, ivA, ivB_1, ivB_2, characteristicsBonus, results);
  }
}

$(document).ready(function () {
  "use strict";
  var $input_yokai_select = $("#input_yokai_select");
  var $input_characteristics = $("#input_characteristics");
  var tabIndex = 10;
  var i = "";

  for (i in statusNamesDict) {
    $("#input_yokai_cb_manual_" + i).attr("tabindex", ++tabIndex);
  }
  for (i in statusNamesDict) {
    $("#input_yokai_iv_a_" + i).attr("tabindex", ++tabIndex);
  }
  for (i in statusNamesDict) {
    $("#input_yokai_iv_b_1_" + i).attr("tabindex", ++tabIndex);
  }
  for (i in statusNamesDict) {
    $("#input_yokai_iv_b_2_" + i).attr("tabindex", ++tabIndex);
  }

  $("#manuallyInput input").prop("disabled", true);

  $input_yokai_select.append($("<option>"));
  $.each(yokaiData, function (i) {
    $input_yokai_select.append(
      $("<option>").text(yokaiData[i].name).attr("data-furigana", yokaiData[i].furigana)
    );
  });
  $.each(characteristicsData, function (i) {
    $input_characteristics.append(
      $("<option>").text(characteristicsData[i].name)
    );
  });
  $input_yokai_select.on("change", updateIVAFilter);
  $input_yokai_select.select2({
    placeholder: "妖怪の種類を選択",
    matcher: function (params, data) {
      var $elm = $(data.element);
      if ($.trim(params.term) === '') {
        return data;
      }
      if ($elm.data("furigana") === undefined) {
        return null;
      } else {
        if ($elm.data("furigana").indexOf(params.term) > -1) {
          return data;
        }
      }
      return null;
    },
    "width": "200px"
  });
  if(/Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent)) {
  } else {
    $input_characteristics.select2({
      "width": "150px",
      minimumResultsForSearch: Infinity
    });
  }

  /* event */
  $("#start_calculation").click(function() {
    calculateStatus(true);
  });
  $("#input_characteristics").change(function (e) {
    $("#manuallyInput input").prop("disabled", true);
    $.each(characteristicsData, function (i) {
      if (characteristicsData[i].name === $(e.target).val() && characteristicsData[i].inputManually) {
        $("#manuallyInput input").prop("disabled", false);
        return false;
      }
    });
  });

  $("#mainTable").on("keyup", "input", calculateStatus);
  $("#mainTable").on("change", "select", calculateStatus);
  $("#manuallyInput").on("keyup", "input", calculateStatus);
  $("#statusTable").on("keyup", "input", calculateStatus);
});
