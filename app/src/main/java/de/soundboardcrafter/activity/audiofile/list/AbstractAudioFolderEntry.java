package de.soundboardcrafter.activity.audiofile.list;

class AbstractAudioFolderEntry {
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return 1;
    }

    @Override
    public String toString() {
        return "AbstractAudioFolderEntry";
    }
}
