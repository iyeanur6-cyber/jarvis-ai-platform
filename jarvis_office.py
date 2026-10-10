import json
import urllib.request
import uno

JARVIS_ENDPOINT = "http://127.0.0.1:8080/api/v1/libreoffice/process"

def _query_jarvis(action, text):
    """Sends payload to Jarvis server and returns response string."""
    payload = json.dumps({"action": action, "text": text}).encode("utf-8")
    req = urllib.request.Request(
        JARVIS_ENDPOINT,
        data=payload,
        headers={"Content-Type": "application/json"}
    )
    try:
        with urllib.request.urlopen(req, timeout=30) as response:
            res_json = json.loads(response.read().decode("utf-8"))
            return res_json.get("result", "")
    except Exception as e:
        return f"\n[Jarvis Error: {str(e)}]\n"

def _get_active_text_selection():
    """Fetches the current text selection and text range object."""
    ctx = uno.getComponentContext()
    smgr = ctx.ServiceManager
    desktop = smgr.createInstanceWithContext("com.sun.star.frame.Desktop", ctx)
    doc = desktop.getCurrentComponent()

    selection = doc.getCurrentSelection()
    if selection and selection.getCount() > 0:
        text_range = selection.getByIndex(0)
        return text_range, text_range.getString()
    return None, ""

# --- LibreOffice UI Macro Triggers ---

def Jarvis_RewriteSelection(*args):
    """Replaces selected text with AI rewritten output."""
    rng, text = _get_active_text_selection()
    if text:
        result = _query_jarvis("rewrite", text)
        rng.setString(result)

def Jarvis_SummarizeSelection(*args):
    """Appends an AI summary right below the selected text."""
    rng, text = _get_active_text_selection()
    if text:
        result = _query_jarvis("summarize", text)
        rng.setString(text + "\n\n--- Jarvis Summary ---\n" + result)

def Jarvis_FixGrammar(*args):
    """Fixes grammar and punctuation in place."""
    rng, text = _get_active_text_selection()
    if text:
        result = _query_jarvis("grammar", text)
        rng.setString(result)

g_exportedScripts = (Jarvis_RewriteSelection, Jarvis_SummarizeSelection, Jarvis_FixGrammar)
