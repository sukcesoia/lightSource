# lightSource

這是一個高功率自行車燈實作紀錄，使用Cree XHP50 19W 2546lm LED作為光源輸出，並搭配簡易光學透鏡產生切線，在控制方面以Arduino為核心基礎，並藉由自行安裝於變把上的觸控模組，快速切換LED輸出功率檔位，此外也結合BLE通訊模組，可透過Android APP控制與取得系統即時資訊。

* [[Part 1] 實作念頭：道路明亮由自己決定](https://sukcesoia.wordpress.com/20…/…/21/lightsource-part01/)
* [[Part 2] 設定目標：想要、需要、必要與規格定義](https://sukcesoia.wordpress.com/20…/…/22/lightsource-part02/)
* [[Part 3] 構想實現：先求有再求好 V1.0](https://sukcesoia.wordpress.com/20…/…/23/lightsource-part03/)
* [[Part 4] 問題修正：學中做做中學 V1.1](https://sukcesoia.wordpress.com/20…/…/24/lightsource-part04/)
* [[Part 5] 版本升級：新增功能與修復問題 V2.0](https://sukcesoia.wordpress.com/20…/…/25/lightsource-part05/)

# 車燈檔位切換

使用安裝在變把撥桿上的觸控模組進行車燈檔位的切換，共有5種檔位分別為 0/255、8/255、64/255、128/255、255/255，若以百分比呈現分別為 0%、3%、25%、50%、100%，若以功率呈現(理論值假設線性)分別為 0W、0.5W、4.8W、9.5W、19W，若以流明呈現(理論值假設線性)分別為 0lm、203.7lm、636.5lm、1273lm、2546lm，而第1檔定義為日行燈，2到4檔照明用，0為關閉。

循環檔位，當常按觸控大於1秒進入第0檔，輕按小於1秒進入第1檔，再次輕按小於1秒進入第2檔，以此類推進入第4檔，到第4檔後輕按小於1秒退後至第3檔，以此類推進入第2檔，當再次輕按小於1秒進入第3檔開始循環，此時永不進入第1檔，除非常按觸控大於1秒進入第0檔，而這機制是為了避免切換過程中必免亮度變化過大影響騎乘。

檔位提示聲，當進入第0檔發出 3600Hz 50ms，第1至3檔發出 2400Hz 100ms，第4檔發出 4000Hz 50ms。

### Demo影片：https://youtu.be/T7ufkqs0UyM

[![IMAGE ALT TEXT HERE](http://img.youtube.com/vi/T7ufkqs0UyM/0.jpg)](https://youtu.be/T7ufkqs0UyM)

 # Adroid APP 遠端控制與系統狀態
 
 使用 Android APP 可顯示即時電壓、電流、功率、目前處於充電或是放電狀態、LED PWM 控制值，也可以控制 LED PWM 覆蓋觸控模組動作。
 
 ### Demo影片：https://youtu.be/hzDxHqgZHls

[![IMAGE ALT TEXT HERE](http://img.youtube.com/vi/hzDxHqgZHls/0.jpg)](https://youtu.be/hzDxHqgZHls)

# 實際上路測試

實測分別在山區、市區道路與自行車道地點測試，並以車燈最大功率輸出19W (2546lm)為測試基準，而做這車燈最重要的目標除了「亮」以外，盡可能不要閃到對向來車，因自己是蠻不喜歡對向來車閃到，影片中也展示車燈切線效果，雖然有明顯切線存在，但光班形狀不適很理想，近距離光班範圍太窄，若調寬後換成遠距離看不到光班，反之遠距離看光班近距離光班範圍太窄，只能取兩者之間比較洽當位置。

### Demo影片：https://youtu.be/KKLe9bshhu4

[![IMAGE ALT TEXT HERE](http://img.youtube.com/vi/KKLe9bshhu4/0.jpg)](https://youtu.be/KKLe9bshhu4)

