use jni::strings::JavaStr;
use std::iter::Peekable;
use std::str::Utf8Error;
use unicode_segmentation::UnicodeSegmentation;

pub struct Graphemes<'a, 'b, 'c>
where
    'a: 'c,
    'b: 'c,
{
    string: *mut JavaStr<'a, 'b>,
    iter: Peekable<unicode_segmentation::Graphemes<'c>>,
}

impl<'a, 'b, 'c> Graphemes<'a, 'b, 'c>
where
    'a: 'c,
    'b: 'c,
{
    pub fn new(string: JavaStr<'a, 'b>) -> Result<Self, Utf8Error> {
        let raw = Box::into_raw(Box::new(string));
        let graphemes = unsafe {
            let str = (*raw).to_str()?;
            str.graphemes(true).peekable()
        };

        Ok(Self {
            string: raw,
            iter: graphemes,
        })
    }

    pub fn has_next(&mut self) -> bool {
        self.iter.peek().is_some()
    }

    pub fn next(&mut self) -> &str {
        self.iter.next().unwrap()
    }
}

impl<'a, 'b, 'c> Drop for Graphemes<'a, 'b, 'c>
where
    'a: 'c,
    'b: 'c,
{
    fn drop(&mut self) {
        unsafe {
            drop(Box::from_raw(self.string));
        }
    }
}
