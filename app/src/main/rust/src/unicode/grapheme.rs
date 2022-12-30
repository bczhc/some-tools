use std::iter::Peekable;

use unicode_segmentation::UnicodeSegmentation;

pub struct Graphemes<'a> {
    string: *mut str,
    iter: Peekable<unicode_segmentation::Graphemes<'a>>,
}

impl<'a> Graphemes<'a> {
    pub fn new(string: String) -> Self {
        let raw = Box::into_raw(string.into_boxed_str());
        let graphemes = unsafe { (*raw).graphemes(true).peekable() };

        Self {
            string: raw,
            iter: graphemes,
        }
    }

    pub fn has_next(&mut self) -> bool {
        self.iter.peek().is_some()
    }

    pub fn next(&mut self) -> &str {
        self.iter.next().unwrap()
    }
}

impl<'a> Drop for Graphemes<'a> {
    fn drop(&mut self) {
        unsafe {
            drop(Box::from_raw(self.string));
        }
    }
}
