#!/usr/bin/env python
# -*- coding: utf-8 -*-

from catenae import Link, Electron
import logging
import re
import lxml.html
import json


class JsonAdapter(Link):
    
    EMOJI_FLAG = 'ba0c66e7cb6f3580'

    def setup(self):
        self.emoji_regex = re.compile(r'alt="(.{1,5})"')
        self.img_regex = re.compile(r'<img([\w\W]+?)/>')

    def get_emojis(self, text):
        return re.findall(self.emoji_regex, text)

    def get_tagged_html(self, text):
        return re.sub(self.img_regex, f'  {JsonAdapter.EMOJI_FLAG}  ', text)

    def insert_emojis(self, emojis, text):
        for emoji in emojis:
            text = text.replace(f' {JsonAdapter.EMOJI_FLAG} ', emoji, 1)
        return text

    def remove_multiple_spaces(self, text):
        # while '  ' in text:
        #     text = text.replace('  ', ' ')
        # return text.strip()
        return re.sub(r'\s+', ' ', text)

    def preprocess_html(self, text):
        new_text = text

        new_text = re.sub(r'\s*<span class="invisible">\s*', '<span>', new_text)
        new_text = re.sub(r'\s*<span class="js-display-url">\s*', '<span>', new_text)
        new_text = re.sub(r'\s+</span>', '</span>', new_text)

        new_text = re.sub(r'\s*<strong>\s*', '<strong>', new_text)
        new_text = re.sub(r'\s+</strong>', '</strong>', new_text)

        new_text = re.sub(r'\s*<s>\s*', '<s>', new_text)
        new_text = re.sub(r'\s+</s>', '</s>', new_text)

        new_text = re.sub(r'\s*<b>\s*', '<b>', new_text)
        new_text = re.sub(r'\s+</b>', '</b>', new_text)

        new_text = re.sub(r'…', '', new_text)

        return new_text

    def get_processed_text(self, html_content):
        emojis = self.get_emojis(html_content)
        preprocessed_html = self.preprocess_html(html_content)
        tagged_html = self.get_tagged_html(preprocessed_html)
        tagged_text = lxml.html.document_fromstring(tagged_html).text_content()
        tagged_text_emojis = self.insert_emojis(emojis, tagged_text)
        postprocessed_text = self.remove_multiple_spaces(tagged_text_emojis)
        return postprocessed_text
        
    def transform(self, electron):
        items = electron.value.split(';')

        user_id = items[0]
        tweet_id = items[1]
        tweet_timestamp = items[2]
        language = items[3]
        html_body = ';'.join(items[4:])
        tweet_body = self.get_processed_text(html_body)
        tweet_url = f'https://twitter.com/{user_id}/status/{tweet_id}'

        electron = Electron(value={'user_id': user_id,
                                   'tweet_id': tweet_id,
                                   'body': tweet_body,
                                   'html_body': html_body,
                                   'timestamp': tweet_timestamp,
                                   'language': language,
                                   'url': tweet_url,
                                   'type': 0,
                                   'src': 'twitter'})
        logging.info(json.dumps(electron.value, indent=4, ensure_ascii=False))
        return electron

if __name__ == "__main__":
    JsonAdapter().start()
