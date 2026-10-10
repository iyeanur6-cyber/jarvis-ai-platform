#!/usr/bin/env python
# -*- coding: utf-8 -*-

import os
import json
import urllib2
from gimpfu import *

JARVIS_GIMP_URL = "http://127.0.0.1:8080/api/v1/gimp/generate"

def jarvis_generate_and_insert_layer(image, drawable, prompt, negative_prompt):
    """
    Sends user prompt to local Jarvis server, waits for the generated image,
    and inserts it as a new layer at the top of the active GIMP canvas.
    """
    if not prompt or prompt.strip() == "":
        gimp.message("Prompt cannot be empty!")
        return

    # 1. Inform the user and start the request
    gimp.progress_init("Connecting to Jarvis & generating image via ComfyUI...")
    gimp.progress_pulse()

    payload = json.dumps({
        "prompt": prompt.strip(),
        "negativePrompt": negative_prompt.strip() if negative_prompt else ""
    })

    req = urllib2.Request(
        JARVIS_GIMP_URL,
        data=payload,
        headers={"Content-Type": "application/json"}
    )

    image_path = None
    try:
        response = urllib2.urlopen(req, timeout=120)
        resp_data = json.loads(response.read())

        if resp_data.get("status") == "SUCCESS":
            image_path = resp_data.get("filePath")
        else:
            gimp.message("Jarvis generation failed: " + str(resp_data.get("message")))
            return

    except Exception as err:
        gimp.message("Failed to communicate with Jarvis server:\n" + str(err))
        return

    # 2. Check if the generated image file exists on disk
    if not image_path or not os.path.exists(image_path):
        gimp.message("Error: Generated image file not found on disk at:\n" + str(image_path))
        return

    gimp.progress_update(0.85)

    # 3. Load the output image directly into the active GIMP canvas
    try:
        image.undo_group_start()

        # Load file as a layer inside the current document context
        new_layer = pdb.gimp_file_load_layer(image, image_path)

        # Truncate prompt for layer label
        layer_name = "Jarvis: " + (prompt[:25] + "..." if len(prompt) > 25 else prompt)
        new_layer.name = layer_name

        # Insert at the top of the layer stack
        pdb.gimp_image_insert_layer(image, new_layer, None, 0)

        # Center layer if dimensions differ from canvas
        if new_layer.width != image.width or new_layer.height != image.height:
            offset_x = (image.width - new_layer.width) // 2
            offset_y = (image.height - new_layer.height) // 2
            new_layer.set_offsets(offset_x, offset_y)

        # Update display
        gimp.displays_flush()
        image.undo_group_end()

    except Exception as err:
        image.undo_group_end()
        gimp.message("Error importing layer into GIMP:\n" + str(err))

# --- Plugin Registration ---
register(
    "python_fu_jarvis_generate_layer",
    "Generate an image using Jarvis (ComfyUI backend) and insert as a layer",
    "Sends prompts to the local Jarvis instance, retrieves the generated image, and inserts it onto the active canvas.",
    "Your Name / Jarvis Community",
    "GPL v3",
    "2026",
    "<Image>/Filters/Jarvis AI/Generate Layer...",
    "*",  # Compatible with RGB, RGBA, Grayscale, etc.
    [
        (PF_STRING, "prompt", "Positive Prompt", "cinematic street photo, rainy night, realistic soft reflections"),
        (PF_STRING, "negative_prompt", "Negative Prompt", "blurry, low quality, oversaturated, deformed, cartoon")
    ],
    [],
    jarvis_generate_and_insert_layer
)

main()
