package br.iesb.vismobile.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import br.iesb.vismobile.R;

/**
 * A simple {@link DialogFragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link SaveFileDialogFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link SaveFileDialogFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SaveFileDialogFragment extends DialogFragment {
    private static final String ARG_DEFAULT = "DEFAULT";

    private String defaultName;
    private OnFragmentInteractionListener listener;


    public SaveFileDialogFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment SaveFileDialogFragment.
     */
    public static SaveFileDialogFragment newInstance(String defaultName) {
        SaveFileDialogFragment fragment = new SaveFileDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_DEFAULT, defaultName);
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Activity activity = getActivity();

        if (getArguments() != null) {
            defaultName = getArguments().getString(ARG_DEFAULT);
        }

        LayoutInflater inflater = activity.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.fragment_save_file_dialog, null);

        if (dialogView == null) {
            return super.onCreateDialog(savedInstanceState);
        }

        final EditText editFilename = (EditText) dialogView.findViewById(R.id.editFilename);
        editFilename.setText(defaultName);

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setView(dialogView)
                .setTitle("Nome do Arquivo")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (listener != null) {
                            listener.onOk(editFilename.getText().toString());
                        }
                    }
                })
                .setNegativeButton("Cancelar", null);

        return builder.create();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            listener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        void onOk(String filename);
    }
}
