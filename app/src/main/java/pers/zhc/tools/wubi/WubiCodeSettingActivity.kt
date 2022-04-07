package pers.zhc.tools.wubi

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.*
import pers.zhc.jni.sqlite.SQLite3
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.Infos
import pers.zhc.tools.R
import pers.zhc.tools.filepicker.FilePicker
import pers.zhc.tools.utils.*
import pers.zhc.tools.views.ProgressView
import java.io.File
import java.net.MalformedURLException
import java.net.URL

class WubiCodeSettingActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val listView = ListView(this)
        setContentView(listView)
        val data = resources.getStringArray(R.array.wubi_code_settings)

        class MyArrayAdapter(context: Context, val resource: Int, objects: Array<out String>) :
            ArrayAdapter<String>(context, resource, objects) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                @Suppress("ViewHolder")
                val view = View.inflate(this@WubiCodeSettingActivity, resource, null)
                val textView = view.findViewById<TextView>(android.R.id.text1)
                textView.text = getItem(position)!!
                return textView
            }
        }

        listView.adapter = MyArrayAdapter(this, android.R.layout.simple_list_item_1, data)
        listView.onItemClickListener = AdapterView.OnItemClickListener { _, view, position, _ ->
            arrayOf(
                View.OnClickListener {

                    downloadWordsDictAction()

                },
                View.OnClickListener {
                    startActivity(Intent(this, WubiDatabaseEditActivity::class.java))
                },
                View.OnClickListener {
                    startActivity(Intent(this, WubiCodeLookingUpActivity::class.java))
                }
            )[position].onClick(view)
        }
    }

    private fun downloadWordsDictAction() {
        val editText = EditText(this)
        editText.setText(
            String.format(
                Infos.githubRawRootURL,
                "bczhc",
                "rime-wubi86-jidian",
                "master",
                "wubi86_jidian.dict"
            )
        )

        DialogUtils.createPromptDialog(this, R.string.wubi_words_dict_url_prompt, { _, et ->

            val urlString = et.text.toString()
            try {
                val url = URL(urlString)

                val wubiDir = File(filesDir, "wubi")
                if (!wubiDir.exists()) {
                    if (!wubiDir.mkdir()) {
                        ToastUtils.show(this, R.string.mkdir_failed)
                        return@createPromptDialog
                    }
                }

                val dictFile = File(wubiDir, "dict")

                Download.startDownloadWithDialog(this, url, dictFile) {
                    // this callback is run in async
                    runOnUiThread {
                        val dialog = Dialog(this)
                        val progressView = ProgressView(this)
                        progressView.setIsIndeterminateMode(true)
                        progressView.setTitle(getString(R.string.processing_msg))
                        dialog.apply {
                            setCanceledOnTouchOutside(false)
                            setContentView(progressView)
                        }
                        dialog.show()

                        Thread {

                            try {
                                processDownloadedDict(dictFile)
                                WubiInverseDictManager.useDatabase {
                                    it.updateUpdateMark(true)
                                }
                                runOnUiThread {
                                    ToastUtils.show(this, R.string.process_done)
                                }
                            } catch (e: Exception) {
                                Common.showException(e, this)
                            }
                            runOnUiThread {
                                dialog.dismiss()
                            }

                        }.start()
                    }

                }

            } catch (e: MalformedURLException) {
                Common.showException(e, this)
                return@createPromptDialog
            }

        }, editText = editText).show()
    }

    private fun initDatabase(db: SQLite3) {
        for (i in 'a'..'z') {
            // language=SQLite
            db.exec(
                """CREATE TABLE IF NOT EXISTS wubi_code_$i
(
    code TEXT NOT NULL PRIMARY KEY,
    word TEXT NOT NULL
)"""
            )
        }
    }

    private fun processDownloadedDict(file: File) {
        val splitRegex = Regex("[\\t ]+")
        val lineCheckRegex = Regex("^.+[\\t ]+[a-z]+$")

        val hashMap = HashMap<String, ArrayList<String>>()

        val reader = file.reader()
        val bufferedReader = reader.buffered()

        val process = { line: String ->
            val split = line.split(splitRegex)
            val code = split[1]
            val word = split[0]

            val get = hashMap[code]
            if (get == null) {
                val arrayList = ArrayList<String>()
                arrayList.add(word)
                hashMap[code] = arrayList
            } else {
                get.add(word)
            }
        }

        while (true) {
            val line = bufferedReader.readLine() ?: throw RuntimeException("Unexpected EOF")
            if (line.matches(lineCheckRegex)) {
                // the first time to meet the words content
                process(line)
                break
            }
        }

        while (true) {
            val line = bufferedReader.readLine()
            line ?: break
            process(line)
        }

        bufferedReader.close()
        reader.close()

        val tempDictDatabaseFile = File(filesDir, "wubi_dict.tmp")
        val db = SQLite3.open(tempDictDatabaseFile.path)
        initDatabase(db)
        db.beginTransaction()

        hashMap.keys.forEach {
            val c = it[0]
            val arrayList = hashMap[it]!!
            val join = arrayList.joinToString("|")
            db.execBind("INSERT INTO wubi_code_$c VALUES (?, ?)", arrayOf(it, join))
        }

        db.commit()
        db.close()

        DictionaryDatabase.changeDatabase(tempDictDatabaseFile.path)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.wubi_dict_settings_activity, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.import_ -> {
                importAction()
            }
            R.id.export -> {
                exportAction()
            }
            else -> {}
        }
        return true
    }

    private val exportFilePickerLauncher = FilePicker.getLauncherWithFilename(this) { path, filename ->
        val file = File(path, filename)
        val dbFile = File(DictionaryDatabase.databasePath)
        FileUtil.copy(dbFile, file)
        ToastUtils.show(this, R.string.exporting_succeeded)
    }

    private fun exportAction() {
        exportFilePickerLauncher.launch(FilePicker.PICK_FOLDER)
    }

    private val importFilePickerLauncher = FilePicker.getLauncher(this) {path->
        DictionaryDatabase.changeDatabase(path ?: return@getLauncher)
        ToastUtils.show(this, R.string.importing_succeeded)
    }

    private fun importAction() {
        importFilePickerLauncher.launch(FilePicker.PICK_FILE)
    }
}
