package pers.zhc.tools.email

import android.content.Context
import android.os.Bundle
import android.view.*
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.email_contact_activity.*
import kotlinx.android.synthetic.main.email_contact_add_dialog.view.*
import kotlinx.android.synthetic.main.email_contact_list_item.view.*
import pers.zhc.jni.sqlite.SQLite3
import pers.zhc.tools.BaseActivity
import pers.zhc.tools.R
import pers.zhc.tools.utils.*

/**
 * @author bczhc
 */
class ContactActivity : BaseActivity() {
    private lateinit var listAdapter: MyAdapter
    private val databaseRef = sharedDatabase.newRefOrCreate()
    private val database = databaseRef.get()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.email_contact_activity)

        val recyclerView = recycler_view!!
        listAdapter = MyAdapter(this, database)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = listAdapter

        listAdapter.setOnItemLongClickListener { position, view ->
            val menu = PopupMenuUtil.create(this, view, R.menu.email_contact_delete_popup)
            menu.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.delete -> {
                        DialogUtils.createConfirmationAlertDialog(this, { _, _ ->
                            listAdapter.delete(position)
                            ToastUtils.show(this, R.string.delete_success)
                        }, titleRes = R.string.delete_confirmation_dialog_title).show()
                    }
                    else -> {
                    }
                }
                return@setOnMenuItemClickListener true
            }
            menu.show()
        }
    }

    class MyAdapter(private val context: Context, private val database: Database) :
        AdapterWithClickListener<MyAdapter.MyHolder>() {
        private var itemData: ItemData = database.queryItemData()

        class MyHolder(val view: View) : RecyclerView.ViewHolder(view) {
            val textView = view.email!!
        }

        override fun onCreateViewHolder(parent: ViewGroup): MyHolder {
            val inflate = LayoutInflater.from(context).inflate(R.layout.email_contact_list_item, parent, false)
            return MyHolder(inflate)
        }

        override fun onBindViewHolder(holder: MyHolder, position: Int) {
            holder.textView.text = itemData[position].toString()
        }

        override fun getItemCount(): Int {
            return itemData.size
        }

        fun add(contact: Contact) {
            database.add(contact)
            itemData.add(contact)
            notifyItemInserted(itemData.size)
        }

        fun delete(email: String) {
            database.delete(email)
            val find = itemData.find {
                return@find it.email == email
            }!!
            val findIndex = itemData.indexOf(find)
            delete(findIndex)
        }

        fun delete(position: Int) {
            itemData.removeAt(position)
            notifyItemRemoved(position)
        }

        fun update(contact: Contact, email: String) {
            database.update(contact, email)
            val find = itemData.find {
                return@find it.email == email
            }!!
            val index = itemData.indexOf(find)
            itemData[index].set(contact)
            notifyItemChanged(index)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.email_contact_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.add -> {
                addContactAction()
            }
            else -> {
            }
        }
        return true
    }

    private fun addContactAction() {
        val inflate = View.inflate(this, R.layout.email_contact_add_dialog, null)
        val nameET = inflate.name_et!!.editText
        val emailET = inflate.email_et!!.editText

        val dialog = DialogUtils.createConfirmationAlertDialog(this, { _, _ ->

            listAdapter.add(Contact(nameET.text.toString(), emailET.text.toString()))
            ToastUtils.show(this, R.string.adding_succeeded)

        }, view = inflate, titleRes = R.string.email_add_contact_dialog_title, width = MATCH_PARENT)

        dialog.show()
    }

    class Database(path: String) {
        val database: SQLite3 = SQLite3.open(path)

        init {
            initDatabase()
        }

        private fun initDatabase() {
            database.exec(
                """CREATE TABLE IF NOT EXISTS contact
(
    name  TEXT NOT NULL,
    email TEXT NOT NULL PRIMARY KEY
)"""
            )
        }

        fun queryItemData(): ItemData {
            val list = ItemData()

            val statement = database.compileStatement("SELECT name, email FROM contact")
            val cursor = statement.cursor
            while (cursor.step()) {
                list.add(Contact(cursor.getText(0), cursor.getText(1)))
            }
            statement.release()

            return list
        }

        fun add(contact: Contact) {
            database.execBind("INSERT INTO contact (name, email) VALUES (?, ?)", arrayOf(contact.name, contact.email))
        }

        fun delete(email: String) {
            database.execBind("DELETE FROM contact WHERE email IS ?", arrayOf(email))
        }

        fun update(newContact: Contact, oldEmail: String) {
            database.execBind("UPDATE contact SET name = ? WHERE email IS ?", arrayOf(newContact.name, oldEmail))
            database.execBind("UPDATE contact SET email = ? WHERE email IS ?", arrayOf(newContact.email, oldEmail))
        }
    }

    override fun finish() {
        databaseRef.release()
        super.finish()
    }

    companion object {
        lateinit var databasePath: String

        class SharedDatabase : SharedDatabaseRef() {
            override fun create(): Database {
                return Database(databasePath)
            }

            override fun close(obj: Database) {
                obj.database.close()
            }
        }

        val sharedDatabase = SharedDatabase()

        fun initPath(context: Context) {
            databasePath = Common.getInternalDatabaseFile(context, "email-contact").path
        }
    }
}

typealias ItemData = ArrayList<Contact>
typealias SharedDatabaseRef = SharedRef<ContactActivity.Database>
