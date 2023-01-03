# Neural Machine Translation Applier

## 1. What is this application
- Separates the text in the document into segments, and <u>applies the machine translation result to the target text.</u>

## 2. Machine Translation Module Support List
1. Google Translation V2
2. Google Translation V3

   <sub>※ Supported NMT module will be added later.</sub>

## 3. Document Type Support List
1. SDL Xliff

   <sub>※ Supported file filters will be added later.</sub>

## 4. How to use
1. This package is an **executable jar** file.
2. First, you have to **build** through **Maven** and **double-click** the jar file created in the Target folder to execute it.
3. Enter the machine translation **API key** or settings in the Settings tab. When using the V3 module, the user can choose to check the *Model* and *Glossary* items and decide whether to use it or not.
![Google Translation Settings](https://github.com/Joseph-Rai/googlenmtapplier/blob/master/src/main/resources/images/Google%20Translation%20Settings.jpg?raw=true)
4. Set the *NMT module*, *file filter*, *source language* and *target language* you want to use.
![NMT Home Settings_1](https://github.com/Joseph-Rai/googlenmtapplier/blob/master/src/main/resources/images/Home%20Settings_1.jpg?raw=true)
5. Basically, it tries to machine translation **based on the source text**. However, depending on the setting value, machine translation may be performed **based on the text already filled in the target**.
6. When you translate based on the text already filled in the target, you can select whether to apply **only the translated text** or to show the **target text and the translated text together**.
![NMT Home Settings_2](https://github.com/Joseph-Rai/googlenmtapplier/blob/master/src/main/resources/images/Home%20Settings_2.jpg?raw=true)
7. Now, *drag-and-drop* the file you want to translate and add it to the file list. If you click the **Open Button**, you can see the current segment status of the file. It is useful to check the condition after applying machine translation.
![File List](https://github.com/Joseph-Rai/googlenmtapplier/blob/master/src/main/resources/images/Home_File_List.jpg?raw=true)
8. Clicking the **Apply NMT button** applies the machine translation result to the added file and saves it.
![Apply NMT Button](https://github.com/Joseph-Rai/googlenmtapplier/blob/master/src/main/resources/images/Home_ApplyNMT_Button.jpg?raw=true)