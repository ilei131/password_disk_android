package com.passworddisk.app.ui

import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.passworddisk.app.R
import com.passworddisk.app.cloud.CloudAccountStore
import com.passworddisk.app.databinding.DialogDeleteAccountBinding
import com.passworddisk.app.viewmodel.PasswordViewModel

/**
 * 删除数据的确认对话框，设置页唯一的删除入口。
 *
 * 一次删干净：本机密码库 + 云端账号（仅当本机记录过云端账号）。
 * 用户名在注册 / 首次同步成功时已存进 [CloudAccountStore]，所以这里只让用户补一次密码；
 * 从未用过云端账号的用户看不到任何输入框，确认后直接删本机数据。
 */
internal fun Fragment.showDeleteDataDialog(viewModel: PasswordViewModel) {
    val username = CloudAccountStore.username(requireContext())
    val dialogBinding = DialogDeleteAccountBinding.inflate(layoutInflater)

    dialogBinding.cloudAccountRow.isVisible = username != null
    dialogBinding.cloudPasswordGroup.isVisible = username != null
    if (username != null) {
        dialogBinding.cloudAccountLabel.text =
            getString(R.string.delete_cloud_account_label, username)
        dialogBinding.dialogMessage.text = getString(R.string.delete_data_message_with_cloud)
    } else {
        dialogBinding.dialogMessage.text = getString(R.string.delete_data_message)
    }

    val dialog = MaterialAlertDialogBuilder(requireContext())
        .setTitle(getString(R.string.delete_data_title))
        .setView(dialogBinding.root)
        .setNegativeButton(getString(R.string.cancel), null)
        .setPositiveButton(getString(R.string.delete_data_confirm), null)
        .create()

    // 接管正按钮：密码没填时不关闭对话框，把错误挂在输入框上
    dialog.setOnShowListener {
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            if (username == null) {
                // 没有云端账号：不需要联网也不需要凭据
                dialog.dismiss()
                viewModel.clearLocalVault()
                return@setOnClickListener
            }

            val password = dialogBinding.cloudPasswordInput.text?.toString().orEmpty()
            dialogBinding.cloudPasswordLayout.error =
                if (password.isEmpty()) getString(R.string.field_required) else null
            if (password.isEmpty()) return@setOnClickListener

            dialog.dismiss()
            viewModel.deleteCloudAndLocalData(password)
        }
    }
    dialog.show()
}
